package com.powers.spell;

import com.powers.PowersBlocks;
import com.powers.config.PowersConfig;
import com.powers.config.PowersConfigLoader;
import com.powers.fx.CelestialRuinFx;
import com.powers.protection.PowerProtection;
import com.powers.util.BoundedEntityCandidates;
import com.powers.util.BoundedSphereCursor;
import com.powers.util.PowerMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Owns persistent, unloaded-chunk-safe Heavenfall countdowns and ruin waves. */
public final class CelestialRuinManager {
	private static final int MAX_ACTIVE_RITUALS = 2;
	private static final int TICKET_TICKS = 4_000;
	private static final int TICKET_RADIUS = CelestialRuinRules.BLAST_RADIUS / 16 + 2;
	private static final int REMOVAL_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS
			| Block.UPDATE_SKIP_ON_PLACE;
	private static final Map<MinecraftServer, List<Ritual>> ACTIVE = new WeakHashMap<>();
	private static final Set<MinecraftServer> LOADED = java.util.Collections.newSetFromMap(new WeakHashMap<>());

	private static final class TicketHolder {
		private static final TicketType CELESTIAL_RUIN = new TicketType(TICKET_TICKS,
				TicketType.FLAG_LOADING | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
	}

	private CelestialRuinManager() {
	}

	/** Refuses overlap abuse while allowing a second catastrophe elsewhere. */
	public static boolean canBegin(ServerLevel level, BlockPos center) {
		loadPersisted(level.getServer());
		List<Ritual> rituals = ACTIVE.getOrDefault(level.getServer(), List.of());
		if (rituals.size() >= MAX_ACTIVE_RITUALS) return false;
		long exclusion = (long) CelestialRuinRules.BLAST_RADIUS * CelestialRuinRules.BLAST_RADIUS * 4L;
		return rituals.stream().noneMatch(ritual -> ritual.level == level
				&& ritual.center.distSqr(center) <= exclusion);
	}

	/** Starts an irreversible one-minute warning and keeps its whole blast area loaded. */
	public static boolean begin(ServerPlayer caster, BlockPos center) {
		ServerLevel level = caster.level();
		if (!canBegin(level, center) || PowerProtection.isSafeZone(level, Vec3.atCenterOf(center))) {
			return false;
		}
		addTicket(level, center);
		Ritual ritual = new Ritual(level, center.immutable(), caster.getUUID(),
				CelestialRuinRules.COUNTDOWN_TICKS, false, null);
		ACTIVE.computeIfAbsent(level.getServer(), ignored -> new ArrayList<>()).add(ritual);
		persist(level.getServer());
		CelestialRuinFx.begins(level, Vec3.atCenterOf(center), CelestialRuinRules.BEAM_RADIUS);
		PowerMessages.sendImportant(caster, "spell.powers.celestial_ruin_begins", 3);
		return true;
	}

	/** Advances countdowns independently of players and persists every state transition. */
	public static void tick(MinecraftServer server) {
		loadPersisted(server);
		List<Ritual> rituals = ACTIVE.get(server);
		if (rituals == null) return;
		Iterator<Ritual> iterator = rituals.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().tick()) iterator.remove();
		}
		if (rituals.isEmpty()) ACTIVE.remove(server);
		persist(server);
	}

	/** Clears only process-local references; SavedData remains authoritative across restarts. */
	public static void clearAll() {
		ACTIVE.clear();
		LOADED.clear();
	}

	private static void addTicket(ServerLevel level, BlockPos center) {
		level.getChunkSource().addTicketWithRadius(TicketHolder.CELESTIAL_RUIN,
				ChunkPos.containing(center), TICKET_RADIUS);
	}

	private static void loadPersisted(MinecraftServer server) {
		if (!LOADED.add(server)) return;
		List<Ritual> restored = new ArrayList<>();
		for (CelestialRuinSavedData.Snapshot snapshot : data(server).snapshots()) {
			Identifier dimension = Identifier.tryParse(snapshot.dimension());
			if (dimension == null) continue;
			ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimension);
			ServerLevel level = server.getLevel(key);
			if (level == null) continue;
			UUID caster;
			try {
				caster = UUID.fromString(snapshot.caster());
			} catch (IllegalArgumentException malformed) {
				continue;
			}
			BlockPos center = new BlockPos(snapshot.x(), snapshot.y(), snapshot.z());
			addTicket(level, center);
			BoundedSphereCursor cursor = snapshot.detonated()
					? new BoundedSphereCursor(snapshot.cursor()) : null;
			restored.add(new Ritual(level, center, caster,
					Math.clamp(snapshot.countdownRemaining(), 0, CelestialRuinRules.COUNTDOWN_TICKS),
					snapshot.detonated(), cursor));
		}
		if (!restored.isEmpty()) ACTIVE.put(server, restored);
		persist(server);
	}

	private static CelestialRuinSavedData data(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(CelestialRuinSavedData.TYPE);
	}

	private static void persist(MinecraftServer server) {
		List<CelestialRuinSavedData.Snapshot> snapshots = ACTIVE
				.getOrDefault(server, List.of()).stream().map(Ritual::snapshot).toList();
		data(server).replace(snapshots);
	}

	private static final class Ritual {
		private final ServerLevel level;
		private final BlockPos center;
		private final UUID caster;
		private int countdownRemaining;
		private boolean detonated;
		private BoundedSphereCursor destruction;

		private Ritual(ServerLevel level, BlockPos center, UUID caster, int countdownRemaining,
				boolean detonated, BoundedSphereCursor destruction) {
			this.level = level;
			this.center = center;
			this.caster = caster;
			this.countdownRemaining = countdownRemaining;
			this.detonated = detonated;
			this.destruction = destruction;
		}

		private boolean tick() {
			if (countdownRemaining > 0) {
				int elapsed = CelestialRuinRules.COUNTDOWN_TICKS - countdownRemaining;
				if (elapsed % 10 == 0) {
					CelestialRuinFx.beam(level, Vec3.atCenterOf(center),
							CelestialRuinRules.BEAM_RADIUS, elapsed);
				}
				warnCaster(elapsed);
				countdownRemaining--;
				if (countdownRemaining > 0) return false;
			}
			if (!chunksReady()) return false;
			if (!detonated) {
				detonate();
				destruction = new BoundedSphereCursor(CelestialRuinRules.BLAST_RADIUS);
				detonated = true;
			}
			destroyBatch();
			if (!destruction.finished()) return false;
			CelestialRuinFx.finished(level, Vec3.atCenterOf(center), CelestialRuinRules.BLAST_RADIUS);
			level.getChunkSource().removeTicketWithRadius(TicketHolder.CELESTIAL_RUIN,
					ChunkPos.containing(center), TICKET_RADIUS);
			return true;
		}

		private boolean chunksReady() {
			ChunkPos origin = ChunkPos.containing(center);
			for (int x = origin.x() - TICKET_RADIUS; x <= origin.x() + TICKET_RADIUS; x++) {
				for (int z = origin.z() - TICKET_RADIUS; z <= origin.z() + TICKET_RADIUS; z++) {
					if (!level.getChunkSource().hasChunk(x, z)) return false;
				}
			}
			return true;
		}

		private CelestialRuinSavedData.Snapshot snapshot() {
			BoundedSphereCursor.Snapshot cursor = destruction == null
					? new BoundedSphereCursor(CelestialRuinRules.BLAST_RADIUS).snapshot()
					: destruction.snapshot();
			return new CelestialRuinSavedData.Snapshot(level.dimension().identifier().toString(),
					center.getX(), center.getY(), center.getZ(), caster.toString(), countdownRemaining,
					detonated, cursor);
		}

		private void warnCaster(int elapsed) {
			int remainingSeconds = (CelestialRuinRules.COUNTDOWN_TICKS - elapsed + 19) / 20;
			if (remainingSeconds != 60 && remainingSeconds != 30 && remainingSeconds != 10
					&& remainingSeconds > 5) return;
			if (elapsed % 20 != 0) return;
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(caster);
			if (player != null) {
				PowerMessages.overlay(player, Component.translatable(
						"spell.powers.celestial_ruin_countdown", remainingSeconds));
			}
		}

		private void detonate() {
			Vec3 epicenter = Vec3.atCenterOf(center);
			CelestialRuinFx.detonates(level, epicenter, CelestialRuinRules.BLAST_RADIUS);
			AABB bounds = AABB.ofSize(epicenter, CelestialRuinRules.BLAST_RADIUS * 2.0,
					CelestialRuinRules.BLAST_RADIUS * 2.0, CelestialRuinRules.BLAST_RADIUS * 2.0);
			List<LivingEntity> entities = BoundedEntityCandidates.living(level, bounds,
					CelestialRuinRules.ENTITY_LIMIT, Entity::isAlive,
					Comparator.comparingDouble((LivingEntity entity) -> entity.distanceToSqr(epicenter))
							.thenComparing(entity -> entity.getUUID().toString()));
			for (LivingEntity entity : entities) {
				if (PowerProtection.isSafeZone(level, entity.position())) continue;
				float damage = CelestialRuinRules.damage(entity.position().distanceTo(epicenter));
				if (damage > 0.0f) entity.hurtServer(level, entity.damageSources().magic(), damage);
			}
		}

		private void destroyBatch() {
			PowersConfig config = PowersConfigLoader.get();
			BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
			for (BoundedSphereCursor.Offset offset : destruction.take(CelestialRuinRules.BLOCKS_PER_TICK)) {
				target.set(center.getX() + offset.x(), center.getY() + offset.y(), center.getZ() + offset.z());
				if (target.getY() < level.getMinY() || target.getY() >= level.getMaxY()) continue;
				if (PowerProtection.isSafeZone(level, Vec3.atCenterOf(target))) continue;
				BlockState state = level.getBlockState(target);
				boolean livingForce = state.is(PowersBlocks.DARKNESS) || state.is(PowersBlocks.PURE_LIGHT);
				boolean hasBlockEntity = level.getBlockEntity(target) != null;
				if (!state.isAir() && CelestialRuinRules.shouldDestroy(livingForce,
						config.celestialRuinTerrainDamage(), hasBlockEntity,
						config.celestialRuinBlockEntityDamage())) {
					level.setBlock(target, Blocks.AIR.defaultBlockState(), REMOVAL_FLAGS);
				}
			}
		}
	}
}

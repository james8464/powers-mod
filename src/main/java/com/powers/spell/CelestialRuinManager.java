package com.powers.spell;

import com.powers.PowersMod;
import com.powers.fx.CelestialRuinFx;
import com.powers.protection.PowerProtection;
import com.powers.util.BoundedEntityCandidates;
import com.powers.util.BoundedSphereCursor;
import com.powers.util.PowerMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
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
import java.util.UUID;
import java.util.WeakHashMap;

/** Owns unloaded-chunk-safe Heavenfall countdowns and work-budgeted ruin waves. */
public final class CelestialRuinManager {
	private static final int MAX_ACTIVE_RITUALS = 2;
	private static final int TICKET_TICKS = 4_000;
	private static final int TICKET_RADIUS = CelestialRuinRules.BLAST_RADIUS / 16 + 2;
	private static final int REMOVAL_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS
			| Block.UPDATE_SKIP_ON_PLACE;
	private static final Map<MinecraftServer, List<Ritual>> ACTIVE = new WeakHashMap<>();

	private static final class TicketHolder {
		private static final TicketType CELESTIAL_RUIN = new TicketType(TICKET_TICKS,
				TicketType.FLAG_LOADING | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
	}

	private CelestialRuinManager() {
	}

	/** Refuses overlap abuse while allowing a second catastrophe elsewhere. */
	public static boolean canBegin(ServerLevel level, BlockPos center) {
		List<Ritual> rituals = ACTIVE.getOrDefault(level.getServer(), List.of());
		if (rituals.size() >= MAX_ACTIVE_RITUALS) {
			return false;
		}
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
		level.getChunkSource().addTicketWithRadius(TicketHolder.CELESTIAL_RUIN,
				ChunkPos.containing(center), TICKET_RADIUS);
		Ritual ritual = new Ritual(level, center.immutable(), caster.getUUID(),
				level.getServer().getTickCount());
		ACTIVE.computeIfAbsent(level.getServer(), ignored -> new ArrayList<>()).add(ritual);
		CelestialRuinFx.begins(level, Vec3.atCenterOf(center), CelestialRuinRules.BEAM_RADIUS);
		PowerMessages.sendImportant(caster, "spell.powers.celestial_ruin_begins", 3);
		return true;
	}

	/** Advances countdowns independently of players and removes completed waves. */
	public static void tick(MinecraftServer server) {
		List<Ritual> rituals = ACTIVE.get(server);
		if (rituals == null) {
			return;
		}
		Iterator<Ritual> iterator = rituals.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().tick(server.getTickCount())) {
				iterator.remove();
			}
		}
		if (rituals.isEmpty()) {
			ACTIVE.remove(server);
		}
	}

	public static void clearAll() {
		ACTIVE.clear();
	}

	private static final class Ritual {
		private final ServerLevel level;
		private final BlockPos center;
		private final UUID caster;
		private final int startedAt;
		private BoundedSphereCursor destruction;

		private Ritual(ServerLevel level, BlockPos center, UUID caster, int startedAt) {
			this.level = level;
			this.center = center;
			this.caster = caster;
			this.startedAt = startedAt;
		}

		private boolean tick(int currentTick) {
			int elapsed = Math.max(0, currentTick - startedAt);
			if (elapsed < CelestialRuinRules.COUNTDOWN_TICKS) {
				if (elapsed % 10 == 0) {
					CelestialRuinFx.beam(level, Vec3.atCenterOf(center),
							CelestialRuinRules.BEAM_RADIUS, elapsed);
				}
				warnCaster(elapsed);
				return false;
			}
			if (destruction == null) {
				detonate();
				destruction = new BoundedSphereCursor(CelestialRuinRules.BLAST_RADIUS);
			}
			destroyBatch();
			if (!destruction.finished()) {
				return false;
			}
			CelestialRuinFx.finished(level, Vec3.atCenterOf(center),
					CelestialRuinRules.BLAST_RADIUS);
			return true;
		}

		private void warnCaster(int elapsed) {
			int remainingSeconds = (CelestialRuinRules.COUNTDOWN_TICKS - elapsed + 19) / 20;
			if (remainingSeconds != 60 && remainingSeconds != 30 && remainingSeconds != 10
					&& remainingSeconds > 5) {
				return;
			}
			if (elapsed % 20 != 0) {
				return;
			}
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
				if (PowerProtection.isSafeZone(level, entity.position())) {
					continue;
				}
				float damage = CelestialRuinRules.damage(entity.position().distanceTo(epicenter));
				if (damage > 0.0f) {
					entity.hurtServer(level, entity.damageSources().magic(), damage);
				}
			}
		}

		private void destroyBatch() {
			BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
			for (BoundedSphereCursor.Offset offset
					: destruction.take(CelestialRuinRules.BLOCKS_PER_TICK)) {
				target.set(center.getX() + offset.x(), center.getY() + offset.y(), center.getZ() + offset.z());
				if (target.getY() < level.getMinY() || target.getY() >= level.getMaxY()) {
					continue;
				}
				if (PowerProtection.isSafeZone(level, Vec3.atCenterOf(target))) {
					continue;
				}
				BlockState state = level.getBlockState(target);
				if (!state.isAir()) {
					level.setBlock(target, Blocks.AIR.defaultBlockState(), REMOVAL_FLAGS);
				}
			}
		}
	}
}

package com.powers.spell;

import com.powers.PowersBlocks;
import com.powers.audit.OperatorAudit;
import com.powers.audit.OperatorAuditAction;
import com.powers.audit.OperatorAuditResult;
import com.powers.config.ResolvedPowerPolicy;
import com.powers.fx.CelestialRuinFx;
import com.powers.mind.PersistentDimensionDiagnostics;
import com.powers.protection.PowerProtection;
import com.powers.power.PowerDamage;
import com.powers.power.state.GlobalTimeStopManager;
import com.powers.util.BoundedEntityCandidates;
import com.powers.util.BoundedSphereCursor;
import com.powers.util.PowerMessages;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.level.levelgen.Heightmap;
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
	private static final int JOURNAL_WINDOW_TICKS = 20;
	private static final int TICKET_TICKS = 4_000;
	private static final int TICKET_RADIUS = CelestialRuinRules.BLAST_RADIUS / 16 + 2;
	private static final int REMOVAL_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS
			| Block.UPDATE_SKIP_ON_PLACE;
	private static final Map<MinecraftServer, List<Ritual>> ACTIVE = new WeakHashMap<>();
	private static final Map<MinecraftServer, List<CelestialRuinSavedData.Snapshot>> ORPHANED =
			new WeakHashMap<>();
	private static final Set<MinecraftServer> LOADED = java.util.Collections.newSetFromMap(new WeakHashMap<>());

	private static final class TicketHolder {
		private static final TicketType CELESTIAL_RUIN = new TicketType(TICKET_TICKS,
				TicketType.FLAG_LOADING | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
	}

	private CelestialRuinManager() {
	}

	/** Builds a bounded staging report from current loaded state and configured regions. */
	public static CelestialRuinPreview preview(ServerLevel level, BlockPos center) {
		return CelestialRuinPreviewService.preview(level, center);
	}

	/** Cancels the nearest staged event only while its commit boundary is still open. */
	public static CelestialRuinCancellation cancelNearest(ServerLevel level, Vec3 position) {
		loadPersisted(level.getServer());
		List<Ritual> rituals = ACTIVE.getOrDefault(level.getServer(), List.of());
		Ritual nearest = rituals.stream().filter(ritual -> ritual.level == level)
				.min(Comparator.comparingDouble(ritual -> Vec3.atCenterOf(ritual.center)
						.distanceToSqr(position))).orElse(null);
		if (nearest == null) return CelestialRuinCancellation.NONE;
		if (!CelestialRuinStagingRules.mayCancel(nearest.countdownRemaining, nearest.detonated)) {
			return CelestialRuinCancellation.LOCKED;
		}
		nearest.removeTicket();
		rituals.remove(nearest);
		if (rituals.isEmpty()) ACTIVE.remove(level.getServer());
		persist(level.getServer());
		return CelestialRuinCancellation.CANCELLED;
	}

	/** Refuses overlap abuse while allowing a second catastrophe elsewhere. */
	public static boolean canBegin(ServerLevel level, BlockPos center) {
		if (!level.getWorldBorder().isWithinBounds(center)) return false;
		loadPersisted(level.getServer());
		List<Ritual> rituals = ACTIVE.getOrDefault(level.getServer(), List.of());
		if (rituals.size() >= MAX_ACTIVE_RITUALS) return false;
		long exclusion = (long) CelestialRuinRules.BLAST_RADIUS * CelestialRuinRules.BLAST_RADIUS * 4L;
		return rituals.stream().noneMatch(ritual -> ritual.level == level
				&& ritual.center.distSqr(center) <= exclusion);
	}

	/** Starts an irreversible one-minute warning; chunk loading begins only near impact. */
	public static boolean begin(ServerPlayer caster, BlockPos center) {
		ServerLevel level = caster.level();
		if (!canBegin(level, center) || PowerProtection.isSafeZone(level, Vec3.atCenterOf(center))
				|| !PowerProtection.mayRitual(caster, level, center)) {
			OperatorAudit.record(OperatorAuditAction.CATASTROPHIC_RITUAL,
					OperatorAuditResult.DENIED, caster.getScoreboardName(), "world", "begin");
			return false;
		}
		Ritual ritual = new Ritual(level, center.immutable(), caster.getUUID(),
				CelestialRuinRules.COUNTDOWN_TICKS, false, null, 0,
				"", null, 0);
		ACTIVE.computeIfAbsent(level.getServer(), ignored -> new ArrayList<>()).add(ritual);
		persist(level.getServer());
		CelestialRuinFx.begins(level, Vec3.atCenterOf(center), CelestialRuinRules.BEAM_RADIUS);
		PowerMessages.sendImportant(caster, "spell.powers.celestial_ruin_begins", 3);
		OperatorAudit.record(OperatorAuditAction.CATASTROPHIC_RITUAL,
				OperatorAuditResult.SUCCESS, caster.getScoreboardName(), "world", "begin");
		return true;
	}

	/** Advances countdowns independently of players and persists every state transition. */
	public static void tick(MinecraftServer server) {
		if (!CelestialRuinRules.mayAdvance(GlobalTimeStopManager.isStopped(server))) return;
		loadPersisted(server);
		List<Ritual> rituals = ACTIVE.get(server);
		if (rituals == null) return;
		boolean completed = false;
		Iterator<Ritual> iterator = rituals.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().tick()) {
				iterator.remove();
				completed = true;
			}
		}
		if (rituals.isEmpty()) ACTIVE.remove(server);
		if (completed || server.getTickCount() % 20 == 0) persist(server);
	}

	/** Clears only process-local references; SavedData remains authoritative across restarts. */
	public static void clearAll() {
		ACTIVE.clear();
		ORPHANED.clear();
		LOADED.clear();
	}

	/** Active persisted countdowns/detonations for administrative diagnostics. */
	public static int activeRitualCount(MinecraftServer server) {
		loadPersisted(server);
		return ACTIVE.getOrDefault(server, List.of()).size();
	}

	/** Persisted events awaiting restoration of a removed datapack dimension. */
	public static int orphanedRitualCount(MinecraftServer server) {
		loadPersisted(server);
		return ORPHANED.getOrDefault(server, List.of()).size();
	}

	/** Sum of currently held square ticket footprints; zero during the distant countdown. */
	public static int forcedChunkCount(MinecraftServer server) {
		loadPersisted(server);
		return ACTIVE.getOrDefault(server, List.of()).stream()
				.mapToInt(ritual -> ritual.ticketRadius < 0 ? 0
						: (ritual.ticketRadius * 2 + 1) * (ritual.ticketRadius * 2 + 1))
				.sum();
	}

	private static void loadPersisted(MinecraftServer server) {
		if (!LOADED.add(server)) return;
		List<Ritual> restored = new ArrayList<>();
		List<CelestialRuinSavedData.Snapshot> orphaned = new ArrayList<>();
		for (CelestialRuinSavedData.Snapshot snapshot : data(server).snapshots()) {
			Identifier dimension = Identifier.tryParse(snapshot.dimension());
			if (dimension == null) continue;
			ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimension);
			ServerLevel level = server.getLevel(key);
			if (level == null) {
				orphaned.add(snapshot);
				PersistentDimensionDiagnostics.record("celestial_ruin", snapshot.dimension());
				continue;
			}
			UUID caster;
			try {
				caster = UUID.fromString(snapshot.caster());
			} catch (IllegalArgumentException malformed) {
				continue;
			}
			BlockPos center = new BlockPos(snapshot.x(), snapshot.y(), snapshot.z());
			BoundedSphereCursor cursor = snapshot.detonated()
					? new BoundedSphereCursor(snapshot.cursor()) : null;
			restored.add(new Ritual(level, center, caster,
					Math.clamp(snapshot.countdownRemaining(), 0, CelestialRuinRules.COUNTDOWN_TICKS),
					snapshot.detonated(), cursor, Math.clamp(snapshot.aftershockStep(), 0,
							CelestialRuinRules.aftershockTotalSteps()), snapshot.pendingPhase(),
					snapshot.pendingCursor(), Math.clamp(snapshot.pendingAftershockEnd(), 0,
							CelestialRuinRules.aftershockTotalSteps())));
		}
		if (!restored.isEmpty()) ACTIVE.put(server, restored);
		if (!orphaned.isEmpty()) ORPHANED.put(server, List.copyOf(orphaned));
		persist(server);
	}

	private static CelestialRuinSavedData data(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(CelestialRuinSavedData.TYPE);
	}

	private static void persist(MinecraftServer server) {
		List<CelestialRuinSavedData.Snapshot> snapshots = new ArrayList<>(
				ORPHANED.getOrDefault(server, List.of()));
		snapshots.addAll(ACTIVE.getOrDefault(server, List.of()).stream().map(Ritual::snapshot).toList());
		data(server).replace(snapshots);
	}

	/** Durably records the next destructive window before any world mutation. */
	private static void checkpoint(MinecraftServer server) {
		persist(server);
		server.overworld().getDataStorage().saveAndJoin();
	}

	private static final class Ritual {
		private final ServerLevel level;
		private final BlockPos center;
		private final UUID caster;
		private int countdownRemaining;
		private boolean detonated;
		private BoundedSphereCursor destruction;
		private int aftershockStep;
		private String pendingPhase;
		private BoundedSphereCursor.Snapshot pendingCursor;
		private int pendingAftershockEnd;
		private int ticketRadius = -1;

		private Ritual(ServerLevel level, BlockPos center, UUID caster, int countdownRemaining,
				boolean detonated, BoundedSphereCursor destruction, int aftershockStep,
				String pendingPhase, BoundedSphereCursor.Snapshot pendingCursor,
				int pendingAftershockEnd) {
			this.level = level;
			this.center = center;
			this.caster = caster;
			this.countdownRemaining = countdownRemaining;
			this.detonated = detonated;
			this.destruction = destruction;
			this.aftershockStep = aftershockStep;
			this.pendingPhase = pendingPhase == null ? "" : pendingPhase;
			this.pendingCursor = pendingCursor;
			this.pendingAftershockEnd = Math.max(aftershockStep, pendingAftershockEnd);
		}

		private boolean tick() {
			ensureTicket();
			var borderDecision = com.powers.power.travel.WorldBoundaryRules.eventDecision(
					detonated, level.getWorldBorder().isWithinBounds(center));
			if (borderDecision == com.powers.power.travel.WorldBoundaryRules.EventDecision.CANCEL) {
				removeTicket();
				return true;
			}
			if (countdownRemaining > 0) {
				int elapsed = CelestialRuinRules.COUNTDOWN_TICKS - countdownRemaining;
				if (elapsed % 10 == 0) {
					CelestialRuinFx.beam(level, Vec3.atCenterOf(center),
							CelestialRuinRules.BEAM_RADIUS, elapsed);
				}
				CelestialRuinWarningRuntime.warnCaster(level, caster, elapsed);
				countdownRemaining--;
				if (countdownRemaining > 0) return false;
			}
			if (CelestialRuinWarningRuntime.awaitingChunks(
					level, center, countdownRemaining, detonated, chunksReady())) return false;
			if (!detonated) {
				pendingPhase = "detonation";
				checkpoint(level.getServer());
				detonate();
				destruction = new BoundedSphereCursor(CelestialRuinRules.BLAST_RADIUS);
				detonated = true;
				pendingPhase = "";
				pendingCursor = destruction.snapshot();
				checkpoint(level.getServer());
			}
			destroyBatch();
			destroyAftershockBatch();
			if (!destruction.finished()
					|| aftershockStep < CelestialRuinRules.aftershockTotalSteps()) return false;
			CelestialRuinFx.finished(level, Vec3.atCenterOf(center), CelestialRuinRules.BLAST_RADIUS);
			removeTicket();
			return true;
		}

		private void ensureTicket() {
			int requested = CelestialRuinTicketRules.radiusForCountdown(
					countdownRemaining, detonated, TICKET_RADIUS);
			if (requested == ticketRadius) return;
			removeTicket();
			if (requested >= 0) {
				level.getChunkSource().addTicketWithRadius(TicketHolder.CELESTIAL_RUIN,
						ChunkPos.containing(center), requested);
				ticketRadius = requested;
			}
		}

		private void removeTicket() {
			if (ticketRadius < 0) return;
			level.getChunkSource().removeTicketWithRadius(TicketHolder.CELESTIAL_RUIN,
					ChunkPos.containing(center), ticketRadius);
			ticketRadius = -1;
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
					detonated, cursor, aftershockStep, pendingPhase,
					pendingCursor == null ? cursor : pendingCursor, pendingAftershockEnd);
		}

		private void detonate() {
			Vec3 epicenter = Vec3.atCenterOf(center);
			CelestialRuinFx.detonates(level, epicenter, CelestialRuinRules.BLAST_RADIUS);
			AABB bounds = CelestialRuinRules.damageBounds(
					epicenter, level.getMinY(), level.getMaxY());
			List<LivingEntity> entities = BoundedEntityCandidates.living(level, bounds,
					CelestialRuinRules.ENTITY_LIMIT, Entity::isAlive,
						Comparator.comparingDouble((LivingEntity entity) -> entity.distanceToSqr(epicenter))
								.thenComparing(entity -> entity.getUUID().toString()));
			for (LivingEntity entity : entities) {
				if (!PowerProtection.mayPowerDamage(null, entity)) continue;
				double distance = entity.position().distanceTo(epicenter);
				float damage = CelestialRuinRules.damage(distance);
				if (damage > 0.0f) entity.hurtServer(level, PowerDamage.celestialRuin(level), damage);
				double force = CelestialRuinRules.knockback(distance);
				if (force > 0.0) {
					Vec3 direction = entity.position().subtract(epicenter);
					double horizontal = Math.hypot(direction.x, direction.z);
					double x = horizontal < 1.0E-6 ? 0.0 : direction.x / horizontal;
					double z = horizontal < 1.0E-6 ? 0.0 : direction.z / horizontal;
					entity.push(x * force, Math.min(4.0, 0.6 + force * 0.18), z * force);
					entity.hurtMarked = true;
				}
			}
		}

		private void destroyBatch() {
			if (destruction.finished()) return;
			prepareCraterWindow();
			ResolvedPowerPolicy policy = ResolvedPowerPolicy.resolve(level);
			BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
			for (BoundedSphereCursor.Offset offset : destruction.take(CelestialRuinRules.BLOCKS_PER_TICK)) {
				target.set(center.getX() + offset.x(), center.getY() + offset.y(), center.getZ() + offset.z());
				if (target.getY() < level.getMinY() || target.getY() >= level.getMaxY()) continue;
				if (!level.getWorldBorder().isWithinBounds(target)) continue;
				if (!PowerProtection.mayAffectBlock(level, target)) continue;
				BlockState state = level.getBlockState(target);
				boolean livingForce = state.is(PowersBlocks.DARKNESS) || state.is(PowersBlocks.PURE_LIGHT);
				boolean hasBlockEntity = level.getBlockEntity(target) != null;
				if (!state.isAir() && CelestialRuinRules.shouldDestroy(livingForce,
						policy.celestialRuinTerrainDamage(), hasBlockEntity,
						policy.celestialRuinBlockEntityDamage())) {
					level.setBlock(target, Blocks.AIR.defaultBlockState(), REMOVAL_FLAGS);
				}
			}
			if (destruction.snapshot().equals(pendingCursor)) {
				pendingPhase = "";
				checkpoint(level.getServer());
			}
		}

		private void prepareCraterWindow() {
			if ("crater".equals(pendingPhase) && pendingCursor != null) return;
			BoundedSphereCursor preview = new BoundedSphereCursor(destruction.snapshot());
			preview.take(CelestialRuinRules.BLOCKS_PER_TICK * JOURNAL_WINDOW_TICKS);
			pendingCursor = preview.snapshot();
			pendingPhase = "crater";
			checkpoint(level.getServer());
		}

		/** Carves explosion-power-100-style streaks only through already loaded distant chunks. */
		private void destroyAftershockBatch() {
			if (aftershockStep >= CelestialRuinRules.aftershockTotalSteps()) return;
			prepareAftershockWindow();
			ResolvedPowerPolicy policy = ResolvedPowerPolicy.resolve(level);
			int end = Math.min(pendingAftershockEnd,
					aftershockStep + CelestialRuinRules.AFTERSHOCK_WORK_PER_TICK);
			for (; aftershockStep < end; aftershockStep++) {
				CelestialRuinRules.AftershockOffset offset =
						CelestialRuinRules.aftershockOffset(aftershockStep);
				BlockPos column = new BlockPos(center.getX() + offset.x(), center.getY(),
						center.getZ() + offset.z());
				if (!level.getWorldBorder().isWithinBounds(column)) continue;
				if (!LoadedChunks.contains(level, column)) continue;
				int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
						column.getX(), column.getZ()) - 1;
				if (surfaceY < level.getMinY() || surfaceY >= level.getMaxY()) continue;
				int depth = 1 + Math.floorMod(aftershockStep * 31, 3);
				for (int down = 0; down < depth; down++) {
					BlockPos target = new BlockPos(column.getX(), surfaceY - down, column.getZ());
					if (target.getY() < level.getMinY()
							|| !PowerProtection.mayAffectBlock(level, target)) continue;
					BlockState state = level.getBlockState(target);
					if (!state.isAir() && CelestialRuinRules.shouldDestroy(
							state.is(PowersBlocks.DARKNESS) || state.is(PowersBlocks.PURE_LIGHT),
							policy.celestialRuinTerrainDamage(), level.getBlockEntity(target) != null,
							policy.celestialRuinBlockEntityDamage())) {
						level.setBlock(target, Blocks.AIR.defaultBlockState(), REMOVAL_FLAGS);
					}
				}
				BlockPos fire = new BlockPos(column.getX(), surfaceY - depth + 1, column.getZ());
				if (CelestialRuinRules.shouldIgnite(policy.celestialRuinTerrainDamage(), false)
						&& PowerProtection.mayAffectBlock(level, fire)
						&& level.getBlockState(fire).isAir()
						&& Blocks.FIRE.defaultBlockState().canSurvive(level, fire)) {
					level.setBlock(fire, Blocks.FIRE.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
			if (aftershockStep >= pendingAftershockEnd) {
				checkpoint(level.getServer());
			}
		}

		private void prepareAftershockWindow() {
			if (pendingAftershockEnd > aftershockStep) return;
			pendingAftershockEnd = Math.min(CelestialRuinRules.aftershockTotalSteps(),
					aftershockStep + CelestialRuinRules.AFTERSHOCK_WORK_PER_TICK
						* JOURNAL_WINDOW_TICKS);
			checkpoint(level.getServer());
		}
	}
}

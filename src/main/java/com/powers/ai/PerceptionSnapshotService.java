package com.powers.ai;

import com.powers.player.SkillSystem;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/** One immutable living-entity observation pass per level, spatial cell, and server tick. */
public final class PerceptionSnapshotService {
	private static final double MAX_HORIZONTAL_RADIUS = 48.0;
	private static final double MAX_VERTICAL_RADIUS = 24.0;
	private static final EntityTypeTest<Entity, LivingEntity> LIVING =
			EntityTypeTest.forClass(LivingEntity.class);
	private static final Map<ServerLevel, LevelCache> LEVELS = new HashMap<>();
	private static long queries;
	private static long cacheHits;
	private static long inspections;

	private record CellKey(int chunkX, int sectionY, int chunkZ) { }
	private record Snapshot(List<PerceptionObservation> observations, int inspected,
			int capacity, AABB bounds) { }
	private static final class LevelCache {
		private long tick = Long.MIN_VALUE;
		private final Map<CellKey, Snapshot> cells = new HashMap<>();
	}

	public record Diagnostics(long queries, long cacheHits, long inspections, int cachedCells) { }

	private PerceptionSnapshotService() {
	}

	public static List<PerceptionObservation> observe(ServerLevel level, Vec3 center,
			double horizontalRadius, double verticalRadius, int limit,
			Predicate<PerceptionObservation> predicate, PerceptionQueryProfile profile) {
		queries++;
		long tick = level.getServer().getTickCount();
		LevelCache cache = LEVELS.computeIfAbsent(level, ignored -> new LevelCache());
		if (cache.tick != tick) {
			cache.tick = tick;
			cache.cells.clear();
		}
		CellKey key = key(center);
		int requestedCapacity = profile.inspectionLimit();
		double horizontal = Math.min(MAX_HORIZONTAL_RADIUS, Math.max(0.0, horizontalRadius));
		double vertical = Math.min(MAX_VERTICAL_RADIUS, Math.max(0.0, verticalRadius));
		AABB queryBounds = AABB.ofSize(center, horizontal * 2.0,
				vertical * 2.0, horizontal * 2.0);
		Snapshot snapshot = snapshot(level, cache, key, queryBounds, requestedCapacity);
		return PerceptionSnapshotRules.select(snapshot.observations(), center,
				horizontal, vertical,
				Math.min(requestedCapacity, Math.max(0, limit)), predicate);
	}

	/** Uses a narrow exact bounds for beam lanes and similarly directional queries. */
	public static List<PerceptionObservation> observe(ServerLevel level, AABB queryBounds,
			Vec3 center, int limit, Predicate<PerceptionObservation> predicate,
			PerceptionQueryProfile profile) {
		queries++;
		long tick = level.getServer().getTickCount();
		LevelCache cache = LEVELS.computeIfAbsent(level, ignored -> new LevelCache());
		if (cache.tick != tick) {
			cache.tick = tick;
			cache.cells.clear();
		}
		AABB bounded = clampBounds(queryBounds, center);
		int requestedCapacity = profile.inspectionLimit();
		Snapshot snapshot = snapshot(level, cache, key(center), bounded, requestedCapacity);
		return PerceptionSnapshotRules.selectWithinBounds(snapshot.observations(), bounded, center,
				Math.min(requestedCapacity, Math.max(0, limit)), predicate);
	}

	public static LivingEntity resolve(ServerLevel level, PerceptionObservation observation) {
		return level.getEntity(observation.entityId()) instanceof LivingEntity living
				&& living.isAlive() && !living.isRemoved() ? living : null;
	}

	public static Diagnostics diagnostics() {
		int cells = LEVELS.values().stream().mapToInt(cache -> cache.cells.size()).sum();
		return new Diagnostics(queries, cacheHits, inspections, cells);
	}

	public static void clear() {
		LEVELS.clear();
		queries = 0L;
		cacheHits = 0L;
		inspections = 0L;
	}

	private static Snapshot capture(ServerLevel level, AABB bounds, int capacity) {
		var batch = BoundedEntityCandidates.collectBatch(level, LIVING,
				bounds, capacity, LivingEntity::isAlive);
		List<PerceptionObservation> observations = batch.candidates().stream()
				.map(PerceptionSnapshotService::observe).toList();
		return new Snapshot(observations, batch.inspected(), capacity, bounds);
	}

	private static Snapshot snapshot(ServerLevel level, LevelCache cache, CellKey key,
			AABB queryBounds, int requestedCapacity) {
		Snapshot snapshot = cache.cells.get(key);
		if (!canReuse(snapshot, queryBounds, requestedCapacity)) {
			snapshot = capture(level, queryBounds, requestedCapacity);
			cache.cells.put(key, snapshot);
			inspections += snapshot.inspected();
		} else {
			cacheHits++;
		}
		return snapshot;
	}

	private static boolean canReuse(Snapshot snapshot, AABB queryBounds, int requestedCapacity) {
		if (snapshot == null || PerceptionSnapshotRules.requiresRecapture(
				snapshot.capacity(), requestedCapacity)) return false;
		if (snapshot.bounds().equals(queryBounds)) return true;
		return snapshot.inspected() < snapshot.capacity()
				&& contains(snapshot.bounds(), queryBounds);
	}

	private static AABB clampBounds(AABB bounds, Vec3 center) {
		return new AABB(
				Math.max(bounds.minX, center.x - MAX_HORIZONTAL_RADIUS),
				Math.max(bounds.minY, center.y - MAX_VERTICAL_RADIUS),
				Math.max(bounds.minZ, center.z - MAX_HORIZONTAL_RADIUS),
				Math.min(bounds.maxX, center.x + MAX_HORIZONTAL_RADIUS),
				Math.min(bounds.maxY, center.y + MAX_VERTICAL_RADIUS),
				Math.min(bounds.maxZ, center.z + MAX_HORIZONTAL_RADIUS));
	}

	private static boolean contains(AABB outer, AABB inner) {
		return outer.minX <= inner.minX && outer.minY <= inner.minY && outer.minZ <= inner.minZ
				&& outer.maxX >= inner.maxX && outer.maxY >= inner.maxY && outer.maxZ >= inner.maxZ;
	}

	private static PerceptionObservation observe(LivingEntity entity) {
		var attackDamage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
		return new PerceptionObservation(entity.getUUID(), entity.position(), entity.getEyePosition(),
				entity.isAlive(),
				entity.entityTags().contains(SkillSystem.DARKNESS_TAG), entity instanceof Monster,
				entity instanceof RangedAttackMob, entity.getMaxHealth(),
				attackDamage == null ? 0.0 : attackDamage.getValue());
	}

	private static CellKey key(Vec3 position) {
		return new CellKey(net.minecraft.util.Mth.floor(position.x) >> 4,
				net.minecraft.util.Mth.floor(position.y) >> 4,
				net.minecraft.util.Mth.floor(position.z) >> 4);
	}
}

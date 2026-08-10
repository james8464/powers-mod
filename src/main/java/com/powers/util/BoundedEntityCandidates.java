package com.powers.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** Inspects a hard-capped typed-entity prefix, then filters and orders it. */
public final class BoundedEntityCandidates {
	private static final EntityTypeTest<Entity, LivingEntity> LIVING_TYPE =
			EntityTypeTest.forClass(LivingEntity.class);

	private BoundedEntityCandidates() {
	}

	/** Collects living candidates without allowing rejected bodies to extend traversal. */
	public static List<LivingEntity> living(ServerLevel level, AABB bounds,
			int inspectionLimit, Predicate<? super LivingEntity> eligibility,
			Comparator<? super LivingEntity> order) {
		return collect(level, LIVING_TYPE, bounds, inspectionLimit, eligibility, order);
	}

	/** Collects a bounded living prefix when encounter order is sufficient. */
	public static List<LivingEntity> living(ServerLevel level, AABB bounds,
			int inspectionLimit, Predicate<? super LivingEntity> eligibility) {
		return collect(level, LIVING_TYPE, bounds, inspectionLimit, eligibility);
	}

	/** Creates the typed query token and applies the same hard inspection cap. */
	public static <T extends Entity> List<T> ofClass(ServerLevel level, Class<T> entityClass,
			AABB bounds, int inspectionLimit, Predicate<? super T> eligibility) {
		return collect(level, EntityTypeTest.forClass(entityClass), bounds,
				inspectionLimit, eligibility);
	}

	/**
	 * Lets Minecraft abort after {@code inspectionLimit} typed entities. Filtering
	 * happens afterward so hostile crowds degrade selection instead of server time.
	 */
	public static <T extends Entity> List<T> collect(ServerLevel level,
			EntityTypeTest<Entity, T> type, AABB bounds, int inspectionLimit,
			Predicate<? super T> eligibility, Comparator<? super T> order) {
		List<T> candidates = collect(level, type, bounds, inspectionLimit, eligibility);
		candidates.sort(order);
		return candidates;
	}

	/** Hard-caps typed inspection and then filters without an unnecessary sort. */
	public static <T extends Entity> List<T> collect(ServerLevel level,
			EntityTypeTest<Entity, T> type, AABB bounds, int inspectionLimit,
			Predicate<? super T> eligibility) {
		List<T> candidates = new ArrayList<>(Math.max(0, inspectionLimit));
		if (inspectionLimit <= 0) return candidates;
		level.getEntities(type, bounds, ignored -> true, candidates, inspectionLimit);
		candidates.removeIf(eligibility.negate());
		return candidates;
	}
}

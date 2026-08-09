package com.powers.power.abilities;

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
final class BoundedEntityCandidates {
	private static final EntityTypeTest<Entity, LivingEntity> LIVING_TYPE =
			EntityTypeTest.forClass(LivingEntity.class);

	private BoundedEntityCandidates() {
	}

	/** Collects living candidates without allowing rejected bodies to extend traversal. */
	static List<LivingEntity> living(ServerLevel level, AABB bounds,
			int inspectionLimit, Predicate<? super LivingEntity> eligibility,
			Comparator<? super LivingEntity> order) {
		return collect(level, LIVING_TYPE, bounds, inspectionLimit, eligibility, order);
	}

	/**
	 * Lets Minecraft abort after {@code inspectionLimit} typed entities. Filtering
	 * happens afterward so hostile crowds degrade selection instead of server time.
	 */
	static <T extends Entity> List<T> collect(ServerLevel level,
			EntityTypeTest<Entity, T> type, AABB bounds, int inspectionLimit,
			Predicate<? super T> eligibility, Comparator<? super T> order) {
		List<T> candidates = new ArrayList<>(Math.max(0, inspectionLimit));
		if (inspectionLimit <= 0) return candidates;
		level.getEntities(type, bounds, ignored -> true, candidates, inspectionLimit);
		candidates.removeIf(eligibility.negate());
		candidates.sort(order);
		return candidates;
	}
}

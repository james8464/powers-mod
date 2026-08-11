package com.powers.magic.runtime;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Pure age, ownership, and closest-segment rules for bounded live ray collisions. */
public final class MagicRayCollisionRules {
	public static final int RETENTION_TICKS = 4;
	public static final int MAX_COLLISIONS_PER_TICK = 32;
	public static final int MAX_COLLISIONS_PER_OWNER_PER_TICK = 4;
	public static final int MAX_SEGMENTS_PER_DIMENSION = 256;
	public static final double COLLISION_THICKNESS = 0.75;

	private static final double EPSILON = 1.0E-9;

	private MagicRayCollisionRules() {
	}

	/** Rejects self-rays, cross-dimension rays, future entries, and expired history. */
	public static boolean mayCompare(MagicRaySegment first, MagicRaySegment second, long now) {
		if (first == null || second == null || now < 0L) return false;
		return !first.owner().equals(second.owner())
				&& first.dimension().equals(second.dimension())
				&& first.gameTime() <= now && second.gameTime() <= now
				&& now - first.gameTime() < RETENTION_TICKS
				&& now - second.gameTime() < RETENTION_TICKS;
	}

	/** Returns the midpoint of the closest points when two finite ray capsules overlap. */
	public static Optional<Vec3> intersection(MagicRaySegment first, MagicRaySegment second) {
		if (first == null || second == null) return Optional.empty();
		Vec3 u = first.end().subtract(first.start());
		Vec3 v = second.end().subtract(second.start());
		Vec3 w = first.start().subtract(second.start());
		double a = u.dot(u);
		double b = u.dot(v);
		double c = v.dot(v);
		double d = u.dot(w);
		double e = v.dot(w);
		double denominator = a * c - b * b;

		double firstFraction;
		double secondFraction;
		if (denominator < EPSILON) {
			firstFraction = 0.0;
			secondFraction = Mth.clamp(e / c, 0.0, 1.0);
		} else {
			firstFraction = Mth.clamp((b * e - c * d) / denominator, 0.0, 1.0);
			secondFraction = Mth.clamp((a * e - b * d) / denominator, 0.0, 1.0);
		}

		// Clamping one segment can move the closest point on the other. Reproject
		// once in each direction; convex segments need no iterative solver.
		firstFraction = Mth.clamp((b * secondFraction - d) / a, 0.0, 1.0);
		secondFraction = Mth.clamp((b * firstFraction + e) / c, 0.0, 1.0);
		Vec3 firstPoint = first.start().add(u.scale(firstFraction));
		Vec3 secondPoint = second.start().add(v.scale(secondFraction));
		if (firstPoint.distanceToSqr(secondPoint) > COLLISION_THICKNESS * COLLISION_THICKNESS) {
			return Optional.empty();
		}
		return Optional.of(firstPoint.add(secondPoint).scale(0.5));
	}
}

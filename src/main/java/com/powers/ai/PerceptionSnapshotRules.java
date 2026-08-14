package com.powers.ai;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** Pure deterministic ordering within a bounded captured cohort, plus measurement rules. */
public final class PerceptionSnapshotRules {
	public record Reduction(int baselineInspections, int actualInspections, double fraction) { }

	private PerceptionSnapshotRules() {
	}

	public static List<PerceptionObservation> select(List<PerceptionObservation> observations,
			Vec3 center, double horizontalRadius, double verticalRadius, int limit,
			Predicate<PerceptionObservation> predicate) {
		double horizontalSquared = Math.max(0.0, horizontalRadius) * Math.max(0.0, horizontalRadius);
		double vertical = Math.max(0.0, verticalRadius);
		return observations.stream().filter(PerceptionObservation::alive).filter(predicate)
				.filter(observation -> Math.abs(observation.position().y - center.y) <= vertical)
				.filter(observation -> horizontalDistanceSquared(observation.position(), center)
						<= horizontalSquared)
				.sorted(Comparator
						.comparingDouble((PerceptionObservation observation) ->
								horizontalDistanceSquared(observation.position(), center))
						.thenComparing(PerceptionObservation::entityId))
				.limit(Math.max(0, limit)).toList();
	}

	public static Reduction reduction(int queries, int perQueryLimit, int actualInspections) {
		int baseline = Math.max(0, queries) * Math.max(0, perQueryLimit);
		int actual = Math.max(0, actualInspections);
		double fraction = baseline == 0 ? 0.0 : 1.0 - Math.min(baseline, actual) / (double) baseline;
		return new Reduction(baseline, actual, fraction);
	}

	public static List<PerceptionObservation> selectWithinBounds(
			List<PerceptionObservation> observations, AABB bounds, Vec3 center, int limit,
			Predicate<PerceptionObservation> predicate) {
		return observations.stream().filter(PerceptionObservation::alive).filter(predicate)
				.filter(observation -> bounds.contains(observation.eyePosition()))
				.sorted(Comparator
						.comparingDouble((PerceptionObservation observation) ->
								observation.eyePosition().distanceToSqr(center))
						.thenComparing(PerceptionObservation::entityId))
				.limit(Math.max(0, limit)).toList();
	}

	public static boolean withinSegmentLane(PerceptionObservation observation,
			Vec3 start, Vec3 end, double radius) {
		Vec3 segment = end.subtract(start);
		if (segment.lengthSqr() < 1.0E-8) {
			return observation.eyePosition().distanceToSqr(start) <= radius * radius;
		}
		double t = Math.clamp(observation.eyePosition().subtract(start).dot(segment)
				/ segment.lengthSqr(), 0.0, 1.0);
		return observation.eyePosition().distanceToSqr(start.add(segment.scale(t)))
				<= Math.max(0.0, radius) * Math.max(0.0, radius);
	}

	public static boolean requiresRecapture(int capturedCapacity, int requestedCapacity) {
		return Math.max(0, capturedCapacity) < Math.max(0, requestedCapacity);
	}

	private static double horizontalDistanceSquared(Vec3 first, Vec3 second) {
		double x = first.x - second.x;
		double z = first.z - second.z;
		return x * x + z * z;
	}
}

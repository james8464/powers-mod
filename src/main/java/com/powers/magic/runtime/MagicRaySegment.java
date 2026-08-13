package com.powers.magic.runtime;

import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.UUID;

/** Immutable authoritative ray segment retained briefly for physical collisions. */
public record MagicRaySegment(UUID owner, String action, String dimension,
		Vec3 start, Vec3 end, long gameTime) {
	public MagicRaySegment {
		Objects.requireNonNull(owner, "owner");
		Objects.requireNonNull(action, "action");
		Objects.requireNonNull(dimension, "dimension");
		Objects.requireNonNull(start, "start");
		Objects.requireNonNull(end, "end");
		if (action.isBlank() || dimension.isBlank() || gameTime < 0L
				|| !hasUsableGeometry(start, end)) {
			throw new IllegalArgumentException("Magic ray segments require finite, non-empty geometry");
		}
	}

	/** Distinguishes a legitimate origin-local counter from publishable collision geometry. */
	static boolean hasUsableGeometry(Vec3 start, Vec3 end) {
		return start != null && end != null && finite(start) && finite(end)
				&& start.distanceToSqr(end) >= 1.0E-6;
	}

	private static boolean finite(Vec3 point) {
		return Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
	}
}

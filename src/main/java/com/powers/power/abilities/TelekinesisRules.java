package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;

/** Pure direction and transaction rules for a telekinetic radial release. */
public final class TelekinesisRules {
	private static final double MIN_HORIZONTAL_DISTANCE_SQUARED = 1.0E-6;

	private TelekinesisRules() {
	}

	/** Returns a finite horizontal impulse away from the caster plus an upward lift. */
	public static Vec3 outwardFling(Vec3 caster, Vec3 target, double horizontalStrength,
			double verticalStrength) {
		if (caster == null || target == null || !finite(caster) || !finite(target)
				|| !Double.isFinite(horizontalStrength)
				|| !Double.isFinite(verticalStrength) || horizontalStrength <= 0.0
				|| verticalStrength <= 0.0) return Vec3.ZERO;
		double dx = target.x - caster.x;
		double dz = target.z - caster.z;
		double distanceSquared = dx * dx + dz * dz;
		if (!Double.isFinite(distanceSquared) || distanceSquared <= MIN_HORIZONTAL_DISTANCE_SQUARED) {
			return Vec3.ZERO;
		}
		double scale = horizontalStrength / Math.sqrt(distanceSquared);
		return new Vec3(dx * scale, verticalStrength, dz * scale);
	}

	/** A cast commits only when at least one server-approved target actually changed. */
	public static boolean resolved(int movedEntities, int reflectedProjectiles) {
		return movedEntities > 0 || reflectedProjectiles > 0;
	}

	private static boolean finite(Vec3 vector) {
		return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
	}
}

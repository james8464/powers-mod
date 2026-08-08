package com.powers.force;

import java.util.Objects;

/** Pure affinity, opposition, spherical-boundary, and blast-falloff rules. */
public final class LivingForceRules {
	/** Gameplay response of an entity standing inside a living force aura. */
	public enum Affinity {
		NONE,
		WITHER,
		REFILL
	}

	private LivingForceRules() {
	}

	/** Resolves darkness-tag affinity; pure light currently has no passive aura. */
	public static Affinity affinity(boolean darknessTagged, LivingForceKind force) {
		Objects.requireNonNull(force, "force");
		if (force != LivingForceKind.DARKNESS) return Affinity.NONE;
		return darknessTagged ? Affinity.REFILL : Affinity.WITHER;
	}

	/** Returns whether two living forces mutually annihilate on contact. */
	public static boolean opposes(LivingForceKind first, LivingForceKind second) {
		return Objects.requireNonNull(first, "first") != Objects.requireNonNull(second, "second");
	}

	/** Integer sphere membership used by the bounded clash cursor. */
	public static boolean insideSphere(int x, int y, int z, int radius) {
		if (radius < 0) return false;
		long radiusSquared = (long) radius * radius;
		return (long) x * x + (long) y * y + (long) z * z <= radiusSquared;
	}

	/** Rejects terrain whose state or ownership makes conversion unsafe. */
	public static boolean mayReplace(boolean air, boolean fluid, boolean blockEntity,
			boolean immune, float destroySpeed) {
		return !air && !fluid && !blockEntity && !immune && destroySpeed >= 0.0F;
	}

	/** Quadratic blast falloff with a hard zero at and beyond the radius. */
	public static double clashDamage(double distance, double radius, double peakDamage) {
		if (!Double.isFinite(distance) || !Double.isFinite(radius) || !Double.isFinite(peakDamage)
				|| radius <= 0.0 || peakDamage <= 0.0 || distance >= radius) return 0.0;
		double remaining = 1.0 - Math.max(0.0, distance) / radius;
		return peakDamage * remaining * remaining;
	}

	/** Linear outward impulse with the same hard radius boundary as clash damage. */
	public static double clashImpulse(double distance, double radius, double peakImpulse) {
		if (!Double.isFinite(distance) || !Double.isFinite(radius) || !Double.isFinite(peakImpulse)
				|| radius <= 0.0 || peakImpulse <= 0.0 || distance >= radius) return 0.0;
		return peakImpulse * (1.0 - Math.max(0.0, distance) / radius);
	}
}

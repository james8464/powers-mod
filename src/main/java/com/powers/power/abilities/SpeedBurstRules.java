package com.powers.power.abilities;

import net.minecraft.world.phys.Vec3;

/** Pure finite geometry and timing rules for synchronized kinetic dashes. */
public final class SpeedBurstRules {
	private static final double MIN_DIRECTION_LENGTH_SQUARED = 1.0E-12;

	private SpeedBurstRules() {
	}

	/** Normalizes look direction, applies strength, and caps unsafe vertical acceleration. */
	public static Vec3 dashVector(Vec3 look, double strength, double minimumY, double maximumY) {
		if (look == null || !finite(look) || look.lengthSqr() <= MIN_DIRECTION_LENGTH_SQUARED
				|| !Double.isFinite(strength) || strength <= 0.0
				|| !Double.isFinite(minimumY) || !Double.isFinite(maximumY)
				|| minimumY > maximumY) return Vec3.ZERO;
		Vec3 normalized = look.normalize();
		return new Vec3(normalized.x * strength,
				Math.clamp(normalized.y * strength, minimumY, maximumY),
				normalized.z * strength);
	}

	/** Returns the traversable prefix of ordered body-volume collision samples. */
	public static double lastSafeFraction(boolean... clearSamples) {
		if (clearSamples == null || clearSamples.length == 0) return 0.0;
		int clear = 0;
		while (clear < clearSamples.length && clearSamples[clear]) clear++;
		return clear / (double) clearSamples.length;
	}

	/** True only inside the mastered follow-up window, including its opening but not expiry. */
	public static boolean secondStepAvailable(long opensAt, long expiresAt, long now,
			boolean mastered) {
		return mastered && expiresAt > opensAt && now >= opensAt && now < expiresAt;
	}

	/** Returns a non-negative client countdown without overflowing the wire integer. */
	public static int secondStepRemaining(long expiresAt, long now, boolean mastered) {
		if (!mastered || expiresAt <= now) return 0;
		long remaining;
		try {
			remaining = Math.subtractExact(expiresAt, now);
		} catch (ArithmeticException ignored) {
			return Integer.MAX_VALUE;
		}
		return (int) Math.min(Integer.MAX_VALUE, remaining);
	}

	private static boolean finite(Vec3 vector) {
		return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
	}
}

package com.powers.entity;

/** Pure following thresholds for Orange Crystal combat echoes. */
public final class EchoCloneRules {
	private static final double FOLLOW_DISTANCE_SQUARED = 6.0 * 6.0;
	private static final double TELEPORT_DISTANCE_SQUARED = 24.0 * 24.0;

	private EchoCloneRules() {
	}

	public static boolean shouldFollow(double distanceSquared) {
		return distanceSquared > FOLLOW_DISTANCE_SQUARED;
	}

	public static boolean shouldTeleport(double distanceSquared) {
		return distanceSquared > TELEPORT_DISTANCE_SQUARED;
	}

	/** Owner-aligned echoes never start faction infighting. */
	public static boolean mayTarget(boolean ownerDarkness, boolean targetDarkness,
			boolean radiantGuardian, boolean sameOwner) {
		if (sameOwner) return false;
		if (ownerDarkness) return !targetDarkness;
		return targetDarkness || !radiantGuardian;
	}
}

package com.powers.power.abilities;

/** Pure sharing and lifetime policy for integrity-owned forcefields. */
public final class ForcefieldRules {
	public static final double SHARING_RADIUS = 2.0;

	private ForcefieldRules() {
	}

	public static boolean withinSharingRadius(double distanceSquared) {
		return Double.isFinite(distanceSquared) && distanceSquared >= 0.0
				&& distanceSquared <= SHARING_RADIUS * SHARING_RADIUS;
	}

	/** Shares with player-compatible targets, while excluding ordinary hostile mobs. */
	public static boolean mayShare(boolean compatible, boolean optedOut, double distanceSquared) {
		return compatible && !optedOut && withinSharingRadius(distanceSquared);
	}

	/** Integrity, not elapsed time, is the only ordinary way a forcefield ends. */
	public static long expiryTick() {
		return Long.MAX_VALUE;
	}

	/** Administrative kill and the void remain explicit server recovery mechanisms. */
	public static boolean interceptable(boolean administratorKill, boolean voidDamage, float amount) {
		return !administratorKill && !voidDamage && Float.isFinite(amount) && amount > 0.0F;
	}
}

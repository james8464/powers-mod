package com.powers.power.abilities;

/** Pure durations and transitions for temporary Ice Manipulation ownership. */
public final class IceLifecycleRules {
	private static final int BRITTLE_DURATION_TICKS = 100;
	private static final double ARMOUR_MULTIPLIER = -0.25;

	private IceLifecycleRules() {
	}

	public static int brittleDurationTicks() {
		return BRITTLE_DURATION_TICKS;
	}

	public static double armourMultiplier() {
		return ARMOUR_MULTIPLIER;
	}

	public static long refreshExpiry(long currentExpiry, long now) {
		return Math.max(currentExpiry, now + BRITTLE_DURATION_TICKS);
	}

	public static boolean expired(long now, long expiry) {
		return now >= expiry;
	}

	public static boolean fireBreaksBrittleArmour(boolean fireDamage) {
		return fireDamage;
	}
}

package com.powers.power.abilities;

/** Pure deadline arithmetic shared by anchor application and diagnostics. */
public final class DimensionalAnchorRules {
	private DimensionalAnchorRules() { }

	public static long renewedDeadline(long now, long currentDeadline, int durationTicks) {
		return Math.max(now, currentDeadline) + Math.max(1, durationTicks);
	}

	public static long remainingTicks(long now, long deadline) {
		return Math.max(0L, deadline - now);
	}
}

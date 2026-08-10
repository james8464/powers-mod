package com.powers.spell;

/** Progressive forced-chunk policy for the final five seconds of Heavenfall. */
public final class CelestialRuinTicketRules {
	private static final int PRELOAD_TICKS = 100;

	private CelestialRuinTicketRules() {
	}

	/** Returns {@code -1} while no ticket is required, otherwise a radius from one to maximum. */
	public static int radiusForCountdown(int countdownRemaining, boolean detonated,
			int maximumRadius) {
		int maximum = Math.max(0, maximumRadius);
		if (detonated || countdownRemaining <= 0) return maximum;
		if (countdownRemaining > PRELOAD_TICKS) return -1;
		int elapsed = PRELOAD_TICKS - Math.max(0, countdownRemaining);
		return Math.min(maximum, 1 + elapsed * Math.max(0, maximum - 1) / PRELOAD_TICKS);
	}
}

package com.powers.power.crystals;

/** Pure toggle/deadline policy for the Blue Crystal's global Chrono Stop. */
public final class ChronoStopRules {
	public static final int MAX_DURATION_TICKS = 1_200;

	private ChronoStopRules() {
	}

	public static boolean isSelectionAction(boolean alreadyOwned) {
		return alreadyOwned;
	}

	public static boolean expired(long startedAt, long now) {
		return now - startedAt >= MAX_DURATION_TICKS;
	}
}

package com.powers.progression;

/** Pure thresholds and anti-AFK timing for shared boss encounter credit. */
public final class BossContributionRules {
	public static final double DAMAGE_THRESHOLD = 8.0;
	public static final double SUPPORT_THRESHOLD = 6.0;
	public static final int CONTROL_TICK_THRESHOLD = 20;
	public static final long ACTIVITY_WINDOW_TICKS = 1_200;
	public static final double ENCOUNTER_RADIUS = 96.0;
	public static final int MAX_CONTRIBUTORS = 24;

	private BossContributionRules() { }

	public static boolean qualifies(double damage, double support, int controlTicks) {
		return damage >= DAMAGE_THRESHOLD || support >= SUPPORT_THRESHOLD
				|| controlTicks >= CONTROL_TICK_THRESHOLD;
	}

	public static boolean recent(long lastMeaningfulTick, long resolutionTick) {
		return lastMeaningfulTick >= 0 && resolutionTick >= lastMeaningfulTick
				&& resolutionTick - lastMeaningfulTick <= ACTIVITY_WINDOW_TICKS;
	}

	public static boolean validAmount(double amount) {
		return Double.isFinite(amount) && amount > 0.0;
	}
}

package com.powers.animation;

/** Pure authoritative-time lifecycle rules for casting poses. */
public final class CastingPoseRules {
	public static final int MAX_DURATION_TICKS = 120;
	public static final int MAX_FUTURE_SKEW_TICKS = 5;

	private CastingPoseRules() {
	}

	public static boolean active(long gameTime, CastingPoseEvent event) {
		return gameTime >= event.startGameTime() && gameTime < event.endGameTime();
	}

	public static double progress(long gameTime, CastingPoseEvent event) {
		if (gameTime <= event.startGameTime()) return 0.0;
		return Math.clamp((gameTime - event.startGameTime()) / (double) event.durationTicks(), 0.0, 1.0);
	}
}

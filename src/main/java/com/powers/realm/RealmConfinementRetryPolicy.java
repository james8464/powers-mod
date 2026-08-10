package com.powers.realm;

/** Finite retry/backoff policy for asynchronous realm confinement. */
public final class RealmConfinementRetryPolicy {
	private static final int MAX_FAILURES = 5;
	private static final int BASE_DELAY_TICKS = 100;
	private static final int MAX_DELAY_TICKS = 1600;

	private RealmConfinementRetryPolicy() {
	}

	public static boolean shouldRetry(int failures) {
		return failures >= 0 && failures < MAX_FAILURES;
	}

	public static int delayTicks(int failures) {
		if (failures <= 0) return BASE_DELAY_TICKS;
		int shift = Math.min(failures, 4);
		return Math.min(MAX_DELAY_TICKS, BASE_DELAY_TICKS << shift);
	}
}

package com.powers.companion;

/** Pure priority and frequency rules for Shadow's non-authoritative callouts. */
public final class ShadowCalloutPolicy {
	public static final int CRITICAL_REPEAT_TICKS = 100;
	public static final int NORMAL_CHATTER_TICKS = 200;
	public static final int VERBOSE_CHATTER_TICKS = 80;

	public enum Kind {
		OWNER_DANGER(true), INTRUSION(true), SUPPRESSED(true), ENGAGE(false), DISENGAGE(false);

		private final boolean critical;

		Kind(boolean critical) {
			this.critical = critical;
		}

		public boolean critical() { return critical; }
	}

	private ShadowCalloutPolicy() { }

	public static boolean shouldEmit(Kind kind, ShadowCalloutMode mode, long now,
			long lastKindAt, long lastChatterAt) {
		if (kind.critical()) return elapsed(now, lastKindAt) >= CRITICAL_REPEAT_TICKS;
		if (mode == ShadowCalloutMode.CRITICAL_ONLY) return false;
		long cooldown = mode == ShadowCalloutMode.VERBOSE
				? VERBOSE_CHATTER_TICKS : NORMAL_CHATTER_TICKS;
		return elapsed(now, lastKindAt) >= cooldown && elapsed(now, lastChatterAt) >= cooldown;
	}

	public static String spoken(Kind kind) {
		return switch (kind) {
			case OWNER_DANGER -> "Your life is failing. Fall back now.";
			case INTRUSION -> "A hostile has breached your reach.";
			case SUPPRESSED -> "Amethyst has silenced my power.";
			case ENGAGE -> "I have the threat.";
			case DISENGAGE -> "The immediate threat is gone.";
		};
	}

	public static String visual(Kind kind) {
		return switch (kind) {
			case OWNER_DANGER -> "SHADOW WARNING: OWNER IN DANGER";
			case INTRUSION -> "SHADOW WARNING: HOSTILE CLOSE";
			case SUPPRESSED -> "SHADOW WARNING: AMETHYST SUPPRESSION";
			case ENGAGE -> "SHADOW: ENGAGING";
			case DISENGAGE -> "SHADOW: CLEAR";
		};
	}

	private static long elapsed(long now, long previous) {
		if (now < previous) return Long.MAX_VALUE;
		return now - previous;
	}
}

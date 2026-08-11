package com.powers.cooldown;

import java.util.Locale;

/** Shared conversion of raw cooldown ticks into player-facing time. */
public final class CooldownPresentation {
	private static final long TICKS_PER_SECOND = 20L;

	private CooldownPresentation() {
	}

	/** Whole seconds, rounded up so any positive remainder stays visible. */
	public static long wholeSeconds(long ticks) {
		return ticks <= 0L ? 0L : Math.ceilDiv(ticks, TICKS_PER_SECOND);
	}

	/** Seconds rendered to one decimal place using locale-stable punctuation. */
	public static String tenths(long ticks) {
		return String.format(Locale.ROOT, "%.1f", Math.max(0L, ticks) / (double) TICKS_PER_SECOND);
	}
}

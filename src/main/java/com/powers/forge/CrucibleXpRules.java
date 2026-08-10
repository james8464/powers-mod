package com.powers.forge;

/** Overflow-safe exponential progression for star-bound Crucible weapons. */
public final class CrucibleXpRules {
	public static final int MAX_LEVEL = 30;

	private CrucibleXpRules() {
	}

	/** Total XP threshold for the requested level, saturated for invalid extremes. */
	public static long requiredForLevel(int level) {
		if (level <= 0) return 0L;
		if (level > 63) return Long.MAX_VALUE;
		int shift = level - 1;
		if (shift >= 57) return Long.MAX_VALUE;
		return 100L << shift;
	}

	/** Recomputes the canonical level from stored XP. */
	public static int levelForXp(long xp) {
		long bounded = Math.max(0L, xp);
		int level = 0;
		while (level < MAX_LEVEL && bounded >= requiredForLevel(level + 1)) level++;
		return level;
	}

	/** Adds only positive XP and saturates rather than wrapping at the long boundary. */
	public static long addSaturated(long current, long award) {
		long bounded = Math.max(0L, current);
		if (award <= 0L) return bounded;
		return Long.MAX_VALUE - bounded < award ? Long.MAX_VALUE : bounded + award;
	}
}

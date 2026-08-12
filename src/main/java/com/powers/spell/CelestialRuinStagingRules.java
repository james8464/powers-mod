package com.powers.spell;

/** Pure preview and irreversible-lock policy for operator-staged Heavenfall. */
public final class CelestialRuinStagingRules {
	private static final int LOCK_TICKS = 100;

	private CelestialRuinStagingRules() {
	}

	/** Cancellation closes when progressive chunk preparation begins. */
	public static boolean mayCancel(int countdownRemaining, boolean detonated) {
		return !detonated && countdownRemaining > LOCK_TICKS;
	}

	/** Keeps the irreversible omen visible while its bounded impact window finishes loading. */
	public static boolean shouldSustainWarning(int countdownRemaining, boolean detonated,
			boolean chunksReady) {
		return !detonated && (countdownRemaining > 0 || !chunksReady);
	}

	/** Conservative square chunk footprint for a horizontal radius, saturated on overflow. */
	public static int squareChunkFootprint(int radiusBlocks) {
		long radiusChunks = (Math.max(0L, radiusBlocks) + 15L) / 16L;
		long width = radiusChunks * 2L + 1L;
		long footprint = width > 46_340L ? Integer.MAX_VALUE : width * width;
		return footprint > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) footprint;
	}
}

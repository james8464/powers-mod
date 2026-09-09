package com.powers.companion;

/** Pure admission rules for local navigation to an explicitly remembered point. */
public final class ShadowPlaceNavigationRules {
	public static final double MAX_DISTANCE_SQUARED = 128.0 * 128.0;

	public enum Decision { GO, WRONG_DIMENSION, CHUNK_UNLOADED, TOO_FAR }

	private ShadowPlaceNavigationRules() { }

	public static Decision decide(boolean sameDimension, boolean chunkLoaded,
			double distanceSquared) {
		if (!sameDimension) return Decision.WRONG_DIMENSION;
		if (!chunkLoaded) return Decision.CHUNK_UNLOADED;
		if (!Double.isFinite(distanceSquared) || distanceSquared > MAX_DISTANCE_SQUARED) {
			return Decision.TOO_FAR;
		}
		return Decision.GO;
	}
}

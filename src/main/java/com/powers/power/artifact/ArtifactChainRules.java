package com.powers.power.artifact;

/** Pure lifecycle checks for hostile Night/Covenant Chain tethers. */
public final class ArtifactChainRules {
	private static final double MAX_DISTANCE_SQUARED = 64.0 * 64.0;

	private ArtifactChainRules() {
	}

	public static boolean active(long tick, long expiresAt, boolean ownerAlive,
			boolean targetAlive, double distanceSquared, boolean lineOfSight,
			boolean dampened, boolean sanctuary, boolean protectedTarget) {
		return tick < expiresAt && ownerAlive && targetAlive
				&& Double.isFinite(distanceSquared) && distanceSquared <= MAX_DISTANCE_SQUARED
				&& lineOfSight && !dampened && !sanctuary && !protectedTarget;
	}
}

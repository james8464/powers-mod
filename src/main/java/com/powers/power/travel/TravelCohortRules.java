package com.powers.power.travel;

/** Pure limits used by every consent-free group teleport route. */
public final class TravelCohortRules {
	public static final double RADIUS = 2.0;
	public static final int MAX_SIZE = 16;

	private TravelCohortRules() {
	}

	public static boolean mayCapture(boolean alive, boolean removed, boolean bodyProxy,
			double distanceSquared) {
		return alive && !removed && !bodyProxy && Double.isFinite(distanceSquared)
				&& distanceSquared <= RADIUS * RADIUS;
	}

	public static boolean mayCommit(boolean alive, boolean sameOrigin, double distanceSquared) {
		return alive && sameOrigin && Double.isFinite(distanceSquared)
				&& distanceSquared <= RADIUS * RADIUS;
	}
}

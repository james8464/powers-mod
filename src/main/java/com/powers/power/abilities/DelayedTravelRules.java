package com.powers.power.abilities;

/** Pure lifecycle gates for delayed teleports and their last-moment companions. */
public final class DelayedTravelRules {
	private DelayedTravelRules() {
	}

	public static boolean travellerMayContinue(boolean casterPresent, boolean casterAlive,
			boolean travellerAlive, boolean casterInOrigin, boolean travellerInOrigin,
			boolean casterDampened, boolean travellerDampened) {
		return casterPresent && casterAlive && travellerAlive && casterInOrigin && travellerInOrigin
				&& !casterDampened && !travellerDampened;
	}

	public static boolean companionMayTravel(boolean alive, boolean inOrigin,
			double distanceSquared, double radius) {
		return alive && inOrigin && Double.isFinite(distanceSquared) && distanceSquared >= 0.0
				&& Double.isFinite(radius) && radius >= 0.0 && distanceSquared <= radius * radius;
	}
}

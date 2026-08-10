package com.powers.boss;

/** Immutable, non-identifying snapshot consumed by one bounded boss decision. */
public record FirstVesselEncounterFacts(
		boolean validTarget,
		double distance,
		double verticalSeparation,
		boolean lineOfSight,
		int clusteredTargets,
		int incomingProjectiles,
		double bossHealthRatio,
		boolean targetMoving,
		boolean warded,
		boolean covered,
		String previousAction,
		long variationSeed) {
	public FirstVesselEncounterFacts {
		distance = finiteNonNegative(distance);
		verticalSeparation = finiteNonNegative(verticalSeparation);
		clusteredTargets = Math.clamp(clusteredTargets, 0, FirstVesselRules.MAX_CANDIDATES);
		incomingProjectiles = Math.clamp(incomingProjectiles, 0, FirstVesselRules.MAX_CANDIDATES);
		bossHealthRatio = Double.isFinite(bossHealthRatio)
				? Math.clamp(bossHealthRatio, 0.0, 1.0) : 1.0;
		previousAction = previousAction == null ? "none" : previousAction;
	}

	private static double finiteNonNegative(double value) {
		return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
	}
}

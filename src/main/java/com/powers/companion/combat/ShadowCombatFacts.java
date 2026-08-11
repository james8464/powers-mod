package com.powers.companion.combat;

/** Compact facts gathered by the bounded controller before the pure planner runs. */
public record ShadowCombatFacts(double distance, double targetHealthRatio,
		double meleeDanger, boolean targetRanged, boolean boss,
		double ownerHealthRatio, double shadowHealthRatio, double energyRatio,
		boolean suppressed, boolean allyInFiringLane, ShadowRequestRange preference) {
	public ShadowCombatFacts {
		distance = Math.clamp(distance, 0.0, 256.0);
		targetHealthRatio = ratio(targetHealthRatio);
		meleeDanger = ratio(meleeDanger);
		ownerHealthRatio = ratio(ownerHealthRatio);
		shadowHealthRatio = ratio(shadowHealthRatio);
		energyRatio = ratio(energyRatio);
		preference = preference == null ? ShadowRequestRange.AUTO : preference;
	}

	public ShadowTargetArchetype archetype() {
		if (boss) return ShadowTargetArchetype.BOSS;
		if (targetRanged && targetHealthRatio < 0.65) return ShadowTargetArchetype.FRAGILE_RANGED;
		if (meleeDanger > 0.65) return ShadowTargetArchetype.MELEE_BRUTE;
		return ShadowTargetArchetype.GENERAL;
	}

	public String contextKey(ShadowEngagementMode mode) {
		int distanceBand = distance < 5 ? 0 : distance < 14 ? 1 : 2;
		int energyBand = energyRatio < .2 ? 0 : energyRatio < .6 ? 1 : 2;
		return mode.name().charAt(0) + ":" + archetype().name().charAt(0)
				+ ":" + distanceBand + ":" + energyBand;
	}

	private static double ratio(double value) {
		return Double.isFinite(value) ? Math.clamp(value, 0.0, 1.0) : 0.0;
	}
}

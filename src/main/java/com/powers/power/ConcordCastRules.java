package com.powers.power;

import com.powers.allegiance.AllegianceMatrix;
import com.powers.allegiance.AllegianceReloadListener;

/** Pure temporal, spatial, and safety limits for paired innate casting. */
public final class ConcordCastRules {
	public static final int WINDOW_TICKS = 40;
	public static final double RANGE = 12.0;
	public static final int PAIR_COOLDOWN_TICKS = 200;
	public static final int MAX_RECENT_CASTS = 512;
	public static final int MAX_IMPACT_TARGETS = 24;

	private ConcordCastRules() {
	}

	public static boolean mayConcord(boolean sameAbility, boolean sameFaction,
			boolean differentOwners, double distance, long age, boolean pairCoolingDown) {
		AllegianceMatrix.Relationship relationship = AllegianceReloadListener.snapshot().matrix()
				.resolve(new AllegianceMatrix.Profile(AllegianceMatrix.Faction.RADIANT,
						AllegianceMatrix.Role.PLAYER),
						new AllegianceMatrix.Profile(sameFaction ? AllegianceMatrix.Faction.RADIANT
								: AllegianceMatrix.Faction.DARKNESS, AllegianceMatrix.Role.PARTY), false);
		return sameAbility && relationship == AllegianceMatrix.Relationship.ALLY
				&& differentOwners && Double.isFinite(distance)
				&& distance <= RANGE && age >= 0 && age <= WINDOW_TICKS && !pairCoolingDown;
	}
}

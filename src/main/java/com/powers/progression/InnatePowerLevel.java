package com.powers.progression;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable authored output for one innate power at one skill level.
 * Multipliers begin at one; destruction is a bounded gameplay/work tier.
 */
public record InnatePowerLevel(double damageMultiplier, double rangeMultiplier,
		double durationMultiplier, int destructionTier, double capacityMultiplier,
		Set<String> variants) {
	public InnatePowerLevel {
		if (!finiteAtLeastOne(damageMultiplier) || !finiteAtLeastOne(rangeMultiplier)
				|| !finiteAtLeastOne(durationMultiplier) || !finiteAtLeastOne(capacityMultiplier)
				|| destructionTier < 0 || destructionTier > 10) {
			throw new IllegalArgumentException("Invalid innate level profile");
		}
		variants = Set.copyOf(Objects.requireNonNull(variants, "variants"));
	}

	private static boolean finiteAtLeastOne(double value) {
		return Double.isFinite(value) && value >= 1.0;
	}
}

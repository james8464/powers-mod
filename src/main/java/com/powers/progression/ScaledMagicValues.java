package com.powers.progression;

import java.util.Set;

/**
 * Finite rank-adjusted values for one canonical magic action. Multipliers are
 * retained so implementations with bespoke base damage or radius can consume
 * the same calculation without duplicating progression rules.
 */
public record ScaledMagicValues(int potency, double range, int durationTicks,
		int energyCost, int cooldownTicks, int interactionPriority,
		Set<String> unlockedVariants, double backlashMultiplier,
		double potencyMultiplier, double rangeMultiplier, double durationMultiplier) {
	public ScaledMagicValues {
		unlockedVariants = Set.copyOf(unlockedVariants);
		if (potency < 0 || !Double.isFinite(range) || range < 0 || durationTicks < 0
				|| energyCost < 0 || cooldownTicks < 0 || interactionPriority < 0
				|| !finitePositive(backlashMultiplier) || !finitePositive(potencyMultiplier)
				|| !finitePositive(rangeMultiplier) || !finitePositive(durationMultiplier)) {
			throw new IllegalArgumentException("Scaled magic values must be finite and non-negative");
		}
	}

	private static boolean finitePositive(double value) {
		return Double.isFinite(value) && value >= 0;
	}
}

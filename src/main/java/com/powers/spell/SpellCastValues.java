package com.powers.spell;

import com.powers.progression.ScaledMagicValues;

/**
 * Immutable mechanical values shared by the grimoire suite. Keeping this
 * arithmetic outside world code keeps authored spell values deterministic
 * exactly once and keeps every resulting value finite and bounded.
 */
record SpellCastValues(double targetRange, double fieldRadius, double purificationRadius,
		int wardSuppressionTicks, int potencyTier,
		double channelSpeedMultiplier) {
	private static final double BASE_TARGET_RANGE = 32.0;
	private static final double BASE_FIELD_RADIUS = 7.0;
	private static final double BASE_PURIFICATION_RADIUS = 8.0;
	private static final int BASE_WARD_SUPPRESSION_TICKS = 900;

	SpellCastValues {
		if (!finitePositive(targetRange) || !finitePositive(fieldRadius)
				|| !finitePositive(purificationRadius) || wardSuppressionTicks < 1 || potencyTier < 0
				|| !finitePositive(channelSpeedMultiplier) || channelSpeedMultiplier == 0.0) {
			throw new IllegalArgumentException("Spell values must be finite and positive");
		}
	}

	static SpellCastValues from(ScaledMagicValues scaling) {
		double potency = scaling.potencyMultiplier();
		double range = scaling.rangeMultiplier();
		int tier = Math.min(3, Math.max(0, (int) Math.floor((potency - 1.0) / 0.25)));
		return new SpellCastValues(
				BASE_TARGET_RANGE * range,
				BASE_FIELD_RADIUS * range,
				BASE_PURIFICATION_RADIUS * range,
				scaledInt(BASE_WARD_SUPPRESSION_TICKS, scaling.durationMultiplier()),
				tier,
				Math.sqrt(scaling.durationMultiplier()));
	}

	int channelTicks(int baseTicks) {
		return baseTicks <= 0 ? 0 : Math.max(1, (int) Math.round(baseTicks / channelSpeedMultiplier));
	}

	private static int scaledInt(int base, double multiplier) {
		return Math.max(1, (int) Math.round(base * multiplier));
	}

	private static boolean finitePositive(double value) {
		return Double.isFinite(value) && value >= 0.0;
	}
}

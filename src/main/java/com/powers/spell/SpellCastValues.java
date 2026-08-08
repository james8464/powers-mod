package com.powers.spell;

import com.powers.progression.ScaledMagicValues;

/**
 * Immutable mechanical values shared by the grimoire suite. Keeping this
 * arithmetic outside world code makes rank and ritual amplification compose
 * exactly once and keeps every resulting value finite and bounded.
 */
record SpellCastValues(double targetRange, int durationTicks, float damage,
		double fieldRadius, double purificationRadius, double banishForce,
		int fireSeconds, int wardSuppressionTicks, int potencyTier,
		double channelSpeedMultiplier) {
	private static final double BASE_TARGET_RANGE = 32.0;
	private static final int BASE_DURATION_TICKS = 600;
	private static final float BASE_DAMAGE = 6.0f;
	private static final double BASE_FIELD_RADIUS = 7.0;
	private static final double BASE_PURIFICATION_RADIUS = 8.0;
	private static final double BASE_BANISH_FORCE = 2.5;
	private static final int BASE_FIRE_SECONDS = 6;
	private static final int BASE_WARD_SUPPRESSION_TICKS = 900;

	SpellCastValues {
		if (!finitePositive(targetRange) || durationTicks < 1 || !Float.isFinite(damage) || damage < 0
				|| !finitePositive(fieldRadius) || !finitePositive(purificationRadius)
				|| !finitePositive(banishForce) || fireSeconds < 1
				|| wardSuppressionTicks < 1 || potencyTier < 0
				|| !finitePositive(channelSpeedMultiplier) || channelSpeedMultiplier == 0.0) {
			throw new IllegalArgumentException("Spell values must be finite and positive");
		}
	}

	static SpellCastValues from(ScaledMagicValues scaling, boolean amplified) {
		double amplificationPotency = amplified ? 1.5 : 1.0;
		double amplificationDuration = amplified ? 1.5 : 1.0;
		double potency = scaling.potencyMultiplier() * amplificationPotency;
		double range = scaling.rangeMultiplier();
		double duration = scaling.durationMultiplier() * amplificationDuration;
		int tier = Math.min(3, Math.max(0, (int) Math.floor((potency - 1.0) / 0.25)));
		return new SpellCastValues(
				BASE_TARGET_RANGE * range,
				scaledInt(BASE_DURATION_TICKS, duration),
				(float) (BASE_DAMAGE * potency),
				BASE_FIELD_RADIUS * range * (amplified ? 1.2 : 1.0),
				BASE_PURIFICATION_RADIUS * range * (amplified ? 1.5 : 1.0),
				BASE_BANISH_FORCE * scaling.potencyMultiplier() * (amplified ? 1.6 : 1.0),
				scaledInt(BASE_FIRE_SECONDS, duration),
				scaledInt(BASE_WARD_SUPPRESSION_TICKS,
						scaling.durationMultiplier() * (amplified ? 2.0 : 1.0)),
				tier,
				Math.sqrt(scaling.durationMultiplier() * (amplified ? 1.15 : 1.0)));
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

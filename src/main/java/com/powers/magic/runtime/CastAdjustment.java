package com.powers.magic.runtime;

import com.powers.magic.InteractionResolution;

import java.util.List;

/**
 * Aggregated pre-cast result. Multipliers are bounded after combining every
 * nearby presence so a crowded battlefield cannot cause exponential values.
 *
 * @param allowed whether the new cast may commit
 * @param potencyMultiplier bounded new-cast potency
 * @param durationMultiplier bounded new-cast duration
 * @param rangeMultiplier bounded new-cast range
 * @param resolutions individual interaction evidence
 */
public record CastAdjustment(boolean allowed, double potencyMultiplier,
		double durationMultiplier, double rangeMultiplier,
		List<InteractionResolution> resolutions) {
	/** Copies resolution evidence and validates aggregate values. */
	public CastAdjustment {
		resolutions = List.copyOf(resolutions);
		if (!finiteBounded(potencyMultiplier) || !finiteBounded(durationMultiplier)
				|| !finiteBounded(rangeMultiplier)) {
			throw new IllegalArgumentException("Cast adjustment multipliers must be within 0..2");
		}
	}

	/** Combines caller-first resolution values with a hard 0..2 safety bound. */
	public static CastAdjustment combine(List<InteractionResolution> resolutions) {
		boolean allowed = true;
		double potency = 1.0;
		double duration = 1.0;
		double range = 1.0;
		for (InteractionResolution resolution : resolutions) {
			if (resolution.blocksFirst()) allowed = false;
			potency = clamp(potency * resolution.firstPotencyMultiplier());
			duration = clamp(duration * resolution.firstDurationMultiplier());
			range = clamp(range * resolution.firstRangeMultiplier());
		}
		return new CastAdjustment(allowed, potency, duration, range, resolutions);
	}

	private static double clamp(double value) {
		return Math.max(0.0, Math.min(2.0, value));
	}

	private static boolean finiteBounded(double value) {
		return Double.isFinite(value) && value >= 0.0 && value <= 2.0;
	}
}

package com.powers.magic.fx;

import java.util.Objects;
import java.util.Optional;

/**
 * One immutable instant in semantic magic choreography.
 *
 * @param beat readable narrative phase
 * @param motifOverride optional geometry replacing the action signature
 * @param budgetScale multiplier applied before the hard particle cap
 * @param geometryScale multiplier applied to local-space point positions
 * @param velocityScale multiplier applied to particle movement
 */
public record FxFrame(FxBeat beat, Optional<FxMotif> motifOverride, double budgetScale,
		double geometryScale, double velocityScale) {
	/** Rejects invalid render values at the pure choreography boundary. */
	public FxFrame {
		Objects.requireNonNull(beat, "beat");
		motifOverride = Objects.requireNonNull(motifOverride, "motifOverride");
		if (!finiteNonNegative(budgetScale) || !finiteNonNegative(geometryScale)
				|| !finiteNonNegative(velocityScale)) {
			throw new IllegalArgumentException("FX frame scales must be finite and non-negative");
		}
	}

	private static boolean finiteNonNegative(double value) {
		return Double.isFinite(value) && value >= 0.0;
	}
}

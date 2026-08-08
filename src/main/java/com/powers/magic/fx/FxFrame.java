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
 * @param verticalOffset world-Y placement relative to the semantic event anchor
 * @param orientation local-space plane selection
 */
public record FxFrame(FxBeat beat, Optional<FxMotif> motifOverride, double budgetScale,
		double geometryScale, double velocityScale, double verticalOffset, FxOrientation orientation) {
	/** Rejects invalid render values at the pure choreography boundary. */
	public FxFrame {
		Objects.requireNonNull(beat, "beat");
		motifOverride = Objects.requireNonNull(motifOverride, "motifOverride");
		Objects.requireNonNull(orientation, "orientation");
		if (!finiteNonNegative(budgetScale) || !finiteNonNegative(geometryScale)
				|| !finiteNonNegative(velocityScale)) {
			throw new IllegalArgumentException("FX frame scales must be finite and non-negative");
		}
		if (!Double.isFinite(verticalOffset)) {
			throw new IllegalArgumentException("FX frame offset must be finite");
		}
	}

	private static boolean finiteNonNegative(double value) {
		return Double.isFinite(value) && value >= 0.0;
	}
}

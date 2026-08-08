package com.powers.magic;

import java.util.Objects;

/**
 * Fully specified outcome of one overlap. Multipliers correspond to the
 * caller's first and second definitions; block flags allow the cast pipeline
 * to cancel only the affected side before resources are committed.
 *
 * @param outcome interaction category
 * @param firstPotencyMultiplier potency applied to the first action
 * @param secondPotencyMultiplier potency applied to the second action
 * @param firstDurationMultiplier duration applied to the first action
 * @param secondDurationMultiplier duration applied to the second action
 * @param firstRangeMultiplier range applied to the first action
 * @param secondRangeMultiplier range applied to the second action
 * @param replacementAspect transformed aspect, or {@code null}
 * @param blocksFirst whether the first action must not commit
 * @param blocksSecond whether the second action must not commit
 * @param cue deterministic audiovisual cue
 * @param mechanics concise generated-document explanation
 */
public record InteractionResolution(
		InteractionOutcome outcome,
		double firstPotencyMultiplier,
		double secondPotencyMultiplier,
		double firstDurationMultiplier,
		double secondDurationMultiplier,
		double firstRangeMultiplier,
		double secondRangeMultiplier,
		MagicAspect replacementAspect,
		boolean blocksFirst,
		boolean blocksSecond,
		InteractionCue cue,
		String mechanics) {
	/** Rejects malformed rule output when the resolver is built or queried. */
	public InteractionResolution {
		Objects.requireNonNull(outcome, "outcome");
		Objects.requireNonNull(cue, "cue");
		Objects.requireNonNull(mechanics, "mechanics");
		if (!hasFiniteNonNegative(firstPotencyMultiplier, secondPotencyMultiplier,
				firstDurationMultiplier, secondDurationMultiplier, firstRangeMultiplier, secondRangeMultiplier)) {
			throw new IllegalArgumentException("Interaction multipliers must be finite and non-negative");
		}
		if (mechanics.isBlank()) {
			throw new IllegalArgumentException("Interaction mechanics description is required");
		}
	}

	/** Returns whether all rule multipliers are finite and non-negative. */
	public boolean hasFiniteMultipliers() {
		return hasFiniteNonNegative(firstPotencyMultiplier, secondPotencyMultiplier,
				firstDurationMultiplier, secondDurationMultiplier, firstRangeMultiplier, secondRangeMultiplier);
	}

	private static boolean hasFiniteNonNegative(double... values) {
		for (double value : values) {
			if (!Double.isFinite(value) || value < 0.0) return false;
		}
		return true;
	}
}

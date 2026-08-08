package com.powers.magic;

/**
 * Server-derived environmental facts that can alter a pure interaction rule.
 * Rank priority is passed separately for each side so light/dark contests can
 * be resolved without the resolver reading player attachments.
 *
 * @param wet whether the collision is in water/rain or on a wet target
 * @param grounded whether a grounding ward or conductive earth is present
 * @param protectedArea whether safe-zone policy forbids harmful transformation
 * @param firstRankPriority first caster's bounded rank priority bonus
 * @param secondRankPriority second caster's bounded rank priority bonus
 */
public record InteractionContext(boolean wet, boolean grounded, boolean protectedArea,
		int firstRankPriority, int secondRankPriority) {
	/** Neutral context used for catalogue documentation and ordinary overlap. */
	public static final InteractionContext DEFAULT = new InteractionContext(false, false, false, 0, 0);

	/** Validates bounded rank input supplied by the scaling service. */
	public InteractionContext {
		if (firstRankPriority < 0 || secondRankPriority < 0
				|| firstRankPriority > 100 || secondRankPriority > 100) {
			throw new IllegalArgumentException("Rank interaction priority must be 0..100");
		}
	}
}

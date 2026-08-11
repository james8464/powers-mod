package com.powers.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellTargetRulesTest {
	@Test
	void channeledTargetsMustStillBeAliveVisibleAndInTheCastersDimension() {
		assertTrue(SpellTargetRules.remainsValid(true, true, true, 32.0 * 32.0, 32.0));
		assertFalse(SpellTargetRules.remainsValid(false, true, true, 1.0, 32.0));
		assertFalse(SpellTargetRules.remainsValid(true, false, true, 1.0, 32.0));
		assertFalse(SpellTargetRules.remainsValid(true, true, false, 1.0, 32.0));
	}

	@Test
	void aTargetThatLeavesTheLockedRangeCannotBeHitAtChannelCompletion() {
		assertFalse(SpellTargetRules.remainsValid(true, true, true,
				Math.nextUp(32.0 * 32.0), 32.0));
		assertFalse(SpellTargetRules.remainsValid(true, true, true, Double.NaN, 32.0));
		assertFalse(SpellTargetRules.remainsValid(true, true, true, 1.0, Double.POSITIVE_INFINITY));
	}

	@Test
	void purificationNeverHealsAnUnalliedHostileMob() {
		assertTrue(SpellTargetRules.mayPurify(true, false));
		assertTrue(SpellTargetRules.mayPurify(false, true));
		assertFalse(SpellTargetRules.mayPurify(false, false));
	}
}

package com.powers.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;

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

	@Test
	void dispelReleaseRevalidatesTheExactInspectedField() {
		assertTrue(SpellTargetRules.dispelFieldRemainsValid(true, true, 100, 101, 16.0, 4.0));
		assertFalse(SpellTargetRules.dispelFieldRemainsValid(false, true, 100, 101, 1.0, 4.0));
		assertFalse(SpellTargetRules.dispelFieldRemainsValid(true, false, 100, 101, 1.0, 4.0));
		assertFalse(SpellTargetRules.dispelFieldRemainsValid(true, true, 100, 100, 1.0, 4.0));
		assertFalse(SpellTargetRules.dispelFieldRemainsValid(true, true, 100, 101, 17.0, 4.0));
	}

	@Test void dispelInspectionSkipsAnIllegalNearerField() {
		assertEquals("legal", SpellTargetRules.nearestLegalDispel(List.of(
				new SpellTargetRules.DispelCandidate("illegal", false, 1.0),
				new SpellTargetRules.DispelCandidate("legal", true, 4.0))).orElseThrow().id());
	}
}

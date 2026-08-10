package com.powers.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowSwordSelectionRulesTest {
	@Test
	void onlyTheMenuSentinelOrARealNestedOptionMayMutateSelection() {
		assertTrue(ShadowSwordSelectionRules.validOption(-1, 0));
		assertTrue(ShadowSwordSelectionRules.validOption(0, 4));
		assertTrue(ShadowSwordSelectionRules.validOption(3, 4));
		assertFalse(ShadowSwordSelectionRules.validOption(-2, 4));
		assertFalse(ShadowSwordSelectionRules.validOption(4, 4));
		assertFalse(ShadowSwordSelectionRules.validOption(0, 0));
	}
}

package com.powers.power.crystals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrystalSelectorRulesTest {
	@Test
	void sixModeRadialSelectionIsBoundedAndKeyboardEquivalent() {
		assertTrue(CrystalSelectorRules.validSelection(6, 0));
		assertTrue(CrystalSelectorRules.validSelection(6, 5));
		assertFalse(CrystalSelectorRules.validSelection(6, 6));
		assertEquals(0, CrystalSelectorRules.numberSlot(49, 6));
		assertEquals(5, CrystalSelectorRules.numberSlot(54, 6));
		assertEquals(CrystalSelectorRules.NONE, CrystalSelectorRules.numberSlot(55, 6));
	}
}

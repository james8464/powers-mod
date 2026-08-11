package com.powers.power.crystals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrystalSelectorRulesTest {
	@Test
	void sevenModeRadialSelectionIsBoundedAndKeyboardEquivalent() {
		assertTrue(CrystalSelectorRules.validSelection(7, 0));
		assertTrue(CrystalSelectorRules.validSelection(7, 6));
		assertFalse(CrystalSelectorRules.validSelection(7, 7));
		assertEquals(0, CrystalSelectorRules.numberSlot(49, 7));
		assertEquals(6, CrystalSelectorRules.numberSlot(55, 7));
		assertEquals(CrystalSelectorRules.NONE, CrystalSelectorRules.numberSlot(56, 7));
	}

	@Test
	void responsiveLayoutLeavesRoomForFullButtonsAndHints() {
		CrystalSelectorRules.Layout standard = CrystalSelectorRules.layout(640, 360);
		assertEquals(108, standard.horizontalRadius());
		assertEquals(70, standard.verticalRadius());
		assertEquals(90, standard.buttonWidth());
		assertTrue(standard.titleY() < standard.centerY() - standard.verticalRadius());
		assertTrue(standard.hintY() > standard.centerY() + standard.verticalRadius());
	}
}

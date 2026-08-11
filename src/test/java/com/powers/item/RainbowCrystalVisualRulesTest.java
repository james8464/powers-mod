package com.powers.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RainbowCrystalVisualRulesTest {
	@Test
	void onlyARainbowCrystalRenderedForADarkHolderUsesTheCorruptedModel() {
		assertTrue(RainbowCrystalVisualRules.corrupted(true, true));
		assertFalse(RainbowCrystalVisualRules.corrupted(true, false));
		assertFalse(RainbowCrystalVisualRules.corrupted(false, true));
	}
}

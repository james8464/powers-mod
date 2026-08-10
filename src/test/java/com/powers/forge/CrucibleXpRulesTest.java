package com.powers.forge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CrucibleXpRulesTest {
	@Test
	void thresholdsDoubleAndLevelCapsAtThirty() {
		assertEquals(100L, CrucibleXpRules.requiredForLevel(1));
		assertEquals(200L, CrucibleXpRules.requiredForLevel(2));
		assertEquals(53_687_091_200L, CrucibleXpRules.requiredForLevel(30));
		assertEquals(0, CrucibleXpRules.levelForXp(99));
		assertEquals(1, CrucibleXpRules.levelForXp(100));
		assertEquals(30, CrucibleXpRules.levelForXp(Long.MAX_VALUE));
	}

	@Test
	void additionSaturatesInsteadOfWrapping() {
		assertEquals(Long.MAX_VALUE, CrucibleXpRules.addSaturated(Long.MAX_VALUE - 2, 10));
		assertEquals(25L, CrucibleXpRules.addSaturated(0, 25));
		assertEquals(10L, CrucibleXpRules.addSaturated(10, -4));
	}
}

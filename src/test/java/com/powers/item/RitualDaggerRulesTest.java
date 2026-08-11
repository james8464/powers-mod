package com.powers.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RitualDaggerRulesTest {
	@Test
	void previewMatchesTheAuthoritativeHealthPaymentAndSurvivalFloor() {
		RitualDaggerRules.Preview allowed = RitualDaggerRules.preview(9.0F, 20, 100);
		assertTrue(allowed.allowed());
		assertEquals(4.0F, allowed.healthCost());
		assertEquals(5.0F, allowed.resultingHealth());
		assertEquals(80, allowed.energyGain());

		RitualDaggerRules.Preview refused = RitualDaggerRules.preview(6.0F, 20, 100);
		assertFalse(refused.allowed());
		assertEquals(6.0F, refused.resultingHealth());
		assertEquals(0, refused.energyGain());
	}
}

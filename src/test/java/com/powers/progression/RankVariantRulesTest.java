package com.powers.progression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankVariantRulesTest {
	@Test
	void trueSightPiercesOnlyTheAdditionalRealmVeilGate() {
		assertTrue(RankVariantRules.mayPierceRealmVeil(true, false));
		assertTrue(RankVariantRules.mayPierceRealmVeil(false, true));
		assertFalse(RankVariantRules.mayPierceRealmVeil(false, false));
	}

	@Test
	void darkResurgenceStrengthensAffinityAndDoublesEmergencyPulses() {
		assertEquals(24, RankVariantRules.darknessRefill(24, 900, 1000, false));
		assertEquals(36, RankVariantRules.darknessRefill(24, 900, 1000, true));
		assertEquals(48, RankVariantRules.darknessRefill(24, 250, 1000, true));
		assertEquals(48, RankVariantRules.darknessRefill(24, -10, 1000, true));
	}

	@Test
	void variantRulesClampInvalidAndOverflowingInputs() {
		assertEquals(0, RankVariantRules.darknessRefill(0, 0, 1000, true));
		assertEquals(0, RankVariantRules.darknessRefill(-5, 0, 1000, true));
		assertEquals(15, RankVariantRules.darknessRefill(10, 5, 0, true));
		assertEquals(Integer.MAX_VALUE,
				RankVariantRules.darknessRefill(Integer.MAX_VALUE, 0, 1, true));
	}
}

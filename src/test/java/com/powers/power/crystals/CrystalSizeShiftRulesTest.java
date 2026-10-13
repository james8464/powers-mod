package com.powers.power.crystals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrystalSizeShiftRulesTest {
	@Test
	void yellowCrystalSpansTheApprovedSafeMinimumAndTenfoldMaximum() {
		assertEquals(0.0625, CrystalSizeShiftRules.SMALL_SCALE, 0.00001);
		assertEquals(10.0, CrystalSizeShiftRules.GIANT_SCALE, 0.00001);
		assertEquals(-0.9375, CrystalSizeShiftRules.modifierFor(0.0625), 0.00001);
		assertEquals(9.0, CrystalSizeShiftRules.modifierFor(10.0), 0.00001);
	}

	@Test
	void alternationMountGateAndComfortProfileAreDeterministic() {
		assertEquals(CrystalSizeShiftRules.SMALL_SCALE, CrystalSizeShiftRules.nextScale(2));
		assertEquals(CrystalSizeShiftRules.GIANT_SCALE, CrystalSizeShiftRules.nextScale(1));
		assertFalse(CrystalSizeShiftRules.mayActivate(true));
		assertTrue(CrystalSizeShiftRules.mayActivate(false));
		assertEquals(2.0, CrystalSizeShiftRules.shockwaveRadius(CrystalSizeShiftRules.SMALL_SCALE));
		assertEquals(6.0, CrystalSizeShiftRules.shockwaveRadius(CrystalSizeShiftRules.GIANT_SCALE));
		assertEquals(0.55F, CrystalSizeShiftRules.comfortPitch(CrystalSizeShiftRules.GIANT_SCALE));
	}
}

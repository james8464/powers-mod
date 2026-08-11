package com.powers.power.crystals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrystalSizeShiftRulesTest {
	@Test
	void yellowCrystalSpansTheApprovedSafeMinimumAndTenfoldMaximum() {
		assertEquals(0.0625, CrystalSizeShiftRules.SMALL_SCALE, 0.00001);
		assertEquals(10.0, CrystalSizeShiftRules.GIANT_SCALE, 0.00001);
		assertEquals(-0.9375, CrystalSizeShiftRules.modifierFor(0.0625), 0.00001);
		assertEquals(9.0, CrystalSizeShiftRules.modifierFor(10.0), 0.00001);
	}
}

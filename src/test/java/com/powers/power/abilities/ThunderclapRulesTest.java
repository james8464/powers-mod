package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThunderclapRulesTest {
	@Test
	void waveIsWideForwardFacingAndRangeBounded() {
		assertTrue(ThunderclapRules.inCone(0.0, 10.0, 0.0, 1.0, 32.0));
		assertTrue(ThunderclapRules.inCone(8.0, 10.0, 0.0, 1.0, 32.0));
		assertFalse(ThunderclapRules.inCone(0.0, -10.0, 0.0, 1.0, 32.0));
		assertFalse(ThunderclapRules.inCone(0.0, 33.0, 0.0, 1.0, 32.0));
	}
}

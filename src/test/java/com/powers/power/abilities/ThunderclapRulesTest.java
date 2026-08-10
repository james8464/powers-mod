package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThunderclapRulesTest {
	@Test
	void waveIsWideForwardFacingAndRangeBounded() {
		assertTrue(ThunderclapRules.inCone(0.0, 10.0, 0.0, 1.0, 32.0));
		assertTrue(ThunderclapRules.inCone(8.0, 10.0, 0.0, 1.0, 32.0));
		assertFalse(ThunderclapRules.inCone(0.0, -10.0, 0.0, 1.0, 32.0));
		assertFalse(ThunderclapRules.inCone(0.0, 33.0, 0.0, 1.0, 32.0));
	}

	@Test
	void verticalAimFallsBackToTheCastersYaw() {
		ThunderclapRules.HorizontalDirection south =
				ThunderclapRules.horizontalDirection(0.0, 0.0, 0.0F);
		assertEquals(0.0, south.x(), 1.0E-8);
		assertEquals(1.0, south.z(), 1.0E-8);

		ThunderclapRules.HorizontalDirection west =
				ThunderclapRules.horizontalDirection(0.0, 0.0, 90.0F);
		assertEquals(-1.0, west.x(), 1.0E-8);
		assertEquals(0.0, west.z(), 1.0E-8);
	}
}

package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightRulesTest {
	@Test
	void propulsionSupportsHoverVerticalControlAndRankedSprint() {
		assertEquals(0.0, FlightRules.motion(0.0F, false, false, false,
				false, false, false, false, 0).y(), 0.0001);
		assertEquals(0.9, FlightRules.motion(0.0F, false, false, false,
				false, true, false, false, 0).y(), 0.0001);
		assertEquals(-0.9, FlightRules.motion(0.0F, false, false, false,
				false, false, true, false, 0).y(), 0.0001);
		double base = FlightRules.motion(0.0F, true, false, false,
				false, false, false, false, 0).horizontalSpeed();
		double mastered = FlightRules.motion(0.0F, true, false, false,
				false, false, false, true, 10).horizontalSpeed();
		assertTrue(mastered > base);
	}

	@Test
	void yawAndStrafeProduceNormalizedHorizontalMotion() {
		FlightRules.Motion motion = FlightRules.motion(90.0F, true, false, false,
				true, false, false, true, 5);
		assertTrue(motion.horizontalSpeed() > 1.0);
		assertTrue(Double.isFinite(motion.x()));
		assertTrue(Double.isFinite(motion.z()));
	}
}

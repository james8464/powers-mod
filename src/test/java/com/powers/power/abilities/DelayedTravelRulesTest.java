package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelayedTravelRulesTest {
	@Test
	void delayedTravellerMustRemainAliveAuthorizedAndInTheOrigin() {
		assertTrue(DelayedTravelRules.travellerMayContinue(
				true, true, true, true, true, false, false));
		assertFalse(DelayedTravelRules.travellerMayContinue(
				true, true, true, true, false, false, false));
		assertFalse(DelayedTravelRules.travellerMayContinue(
				true, true, true, true, true, true, false));
	}

	@Test
	void companionsAreRevalidatedAtTheFinalBlink() {
		assertTrue(DelayedTravelRules.companionMayTravel(true, true, 1.0, 1.3));
		assertFalse(DelayedTravelRules.companionMayTravel(true, false, 0.0, 1.3));
		assertFalse(DelayedTravelRules.companionMayTravel(true, true, 4.0, 1.3));
		assertFalse(DelayedTravelRules.companionMayTravel(true, true, Double.NaN, 1.3));
	}
}

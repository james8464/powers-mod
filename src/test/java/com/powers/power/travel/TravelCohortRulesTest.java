package com.powers.power.travel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelCohortRulesTest {
	@Test
	void cohortIsTwoBlocksWideAndHardCappedAtSixteenLivingTravellers() {
		assertEquals(2.0, TravelCohortRules.RADIUS);
		assertEquals(16, TravelCohortRules.MAX_SIZE);
		assertTrue(TravelCohortRules.mayCapture(true, false, false, 4.0));
		assertFalse(TravelCohortRules.mayCapture(true, false, false, 4.000_001));
		assertFalse(TravelCohortRules.mayCapture(false, false, false, 0.0));
		assertFalse(TravelCohortRules.mayCapture(true, true, false, 0.0));
		assertFalse(TravelCohortRules.mayCapture(true, false, true, 0.0));
	}

	@Test
	void delayedCommitRequiresTheSameOriginAndRangeButNeverConsent() {
		assertTrue(TravelCohortRules.mayCommit(true, true, 4.0));
		assertFalse(TravelCohortRules.mayCommit(false, true, 0.0));
		assertFalse(TravelCohortRules.mayCommit(true, false, 0.0));
		assertFalse(TravelCohortRules.mayCommit(true, true, 4.01));
	}

	@Test
	void exactCoordinatesSkipOnlyEnvironmentalLandingSafety() {
		assertFalse(SafeDestinationResolver.environmentalSafetyRequired(
				SafeDestinationResolver.DestinationMode.EXACT));
		assertTrue(SafeDestinationResolver.environmentalSafetyRequired(
				SafeDestinationResolver.DestinationMode.SAFE_LANDING));
	}
}

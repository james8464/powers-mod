package com.powers.power.crystals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChronoStopRulesTest {
	@Test
	void crystalStopMayBeToggledOffAndHardStopsAtOneMinute() {
		assertTrue(ChronoStopRules.isSelectionAction(true));
		assertFalse(ChronoStopRules.isSelectionAction(false));
		assertFalse(ChronoStopRules.expired(1_000, 2_199));
		assertTrue(ChronoStopRules.expired(1_000, 2_200));
	}
}

package com.powers.power.travel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldBoundaryRulesTest {
	@Test
	void contractionClampsRecoveriesButRejectsOrdinaryDestinations() {
		assertEquals(99.0, WorldBoundaryRules.clampCoordinate(140.0, -100.0, 100.0, 1.0));
		assertEquals(-99.0, WorldBoundaryRules.clampCoordinate(-140.0, -100.0, 100.0, 1.0));
		assertEquals(12.0, WorldBoundaryRules.clampCoordinate(12.0, -100.0, 100.0, 1.0));
		assertTrue(WorldBoundaryRules.validSpan(-100.0, 100.0, 1.0));
		assertFalse(WorldBoundaryRules.validSpan(0.0, 1.0, 1.0));
	}

	@Test
	void committedRuinClipsWhileUncommittedEventsCancel() {
		assertEquals(WorldBoundaryRules.EventDecision.CANCEL,
				WorldBoundaryRules.eventDecision(false, false));
		assertEquals(WorldBoundaryRules.EventDecision.COMPLETE_CLIPPED,
				WorldBoundaryRules.eventDecision(true, false));
		assertEquals(WorldBoundaryRules.EventDecision.CONTINUE,
				WorldBoundaryRules.eventDecision(false, true));
	}
}

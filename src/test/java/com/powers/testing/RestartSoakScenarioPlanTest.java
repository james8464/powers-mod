package com.powers.testing;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestartSoakScenarioPlanTest {
	@Test
	void everyAcceptanceCycleExercisesAllRequiredLongLivedSystems() {
		assertEquals(Set.of(
				RestartSoakScenarioPlan.System.TRAVEL_TICKET,
				RestartSoakScenarioPlan.System.BODY_PROXY,
				RestartSoakScenarioPlan.System.SPELL_FIELD,
				RestartSoakScenarioPlan.System.GUARDIAN_SUMMON,
				RestartSoakScenarioPlan.System.LIVING_FORCE_INDEX,
				RestartSoakScenarioPlan.System.TIME_FREEZE,
				RestartSoakScenarioPlan.System.CELESTIAL_RUIN,
				RestartSoakScenarioPlan.System.CONNECTED_CLIENT),
				RestartSoakScenarioPlan.requiredSystems());
	}

	@Test
	void theTwelfthCycleUsesFlushedSigtermAndOtherCyclesStopCleanly() {
		assertEquals(RestartSoakScenarioPlan.Shutdown.CLEAN_STOP,
				RestartSoakScenarioPlan.shutdownForCycle(1));
		assertEquals(RestartSoakScenarioPlan.Shutdown.CLEAN_STOP,
				RestartSoakScenarioPlan.shutdownForCycle(11));
		assertEquals(RestartSoakScenarioPlan.Shutdown.FLUSHED_SIGTERM,
				RestartSoakScenarioPlan.shutdownForCycle(12));
		assertEquals(RestartSoakScenarioPlan.Shutdown.FLUSHED_SIGTERM,
				RestartSoakScenarioPlan.shutdownForCycle(24));
	}

	@Test
	void rolloverLeavesEnoughTimeToPersistBeforeTheFiveMinuteBoundary() {
		assertEquals(30, RestartSoakScenarioPlan.rolloverLeadSeconds(300));
		assertEquals(10, RestartSoakScenarioPlan.rolloverLeadSeconds(20));
		assertEquals(5, RestartSoakScenarioPlan.rolloverLeadSeconds(10));
	}
}

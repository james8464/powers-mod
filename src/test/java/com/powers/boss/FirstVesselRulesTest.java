package com.powers.boss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirstVesselRulesTest {
	@Test
	void phasesAndMultiplayerScalingAreBounded() {
		assertEquals(FirstVesselPhase.AWAKENING, FirstVesselRules.phase(0.71));
		assertEquals(FirstVesselPhase.UNBOUND, FirstVesselRules.phase(0.70));
		assertEquals(FirstVesselPhase.UNBOUND, FirstVesselRules.phase(0.36));
		assertEquals(FirstVesselPhase.LAST_COVENANT, FirstVesselRules.phase(0.35));
		assertEquals(1.0, FirstVesselRules.playerScale(0));
		assertEquals(1.0, FirstVesselRules.playerScale(1));
		assertEquals(2.1, FirstVesselRules.playerScale(3));
		assertEquals(4.0, FirstVesselRules.playerScale(99));
	}

	@Test
	void reconstitutionIsOnceOnlyAndInterruptibleByMeaningfulDamage() {
		assertTrue(FirstVesselRules.shouldBeginReconstitution(0.49, false));
		assertFalse(FirstVesselRules.shouldBeginReconstitution(0.51, false));
		assertFalse(FirstVesselRules.shouldBeginReconstitution(0.20, true));
		assertFalse(FirstVesselRules.channelInterrupted(399.9F, 5_000.0F));
		assertTrue(FirstVesselRules.channelInterrupted(400.0F, 5_000.0F));
	}

	@Test
	void tacticalWorkRunsOnOneBoundedCadence() {
		assertTrue(FirstVesselRules.planningTick(100));
		assertFalse(FirstVesselRules.planningTick(101));
		assertEquals(24, FirstVesselRules.MAX_CANDIDATES);
	}

	@Test
	void hostileControlStillYieldsToCounterplay() {
		assertTrue(FirstVesselRules.mayControl(false, false, false));
		assertFalse(FirstVesselRules.mayControl(true, false, false));
		assertFalse(FirstVesselRules.mayControl(false, true, false));
		assertFalse(FirstVesselRules.mayControl(false, false, true));
	}
}

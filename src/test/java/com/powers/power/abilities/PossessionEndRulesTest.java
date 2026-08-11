package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PossessionEndRulesTest {
	@Test
	void deadVesselHasPriorityOverExpiryAndProtectionChanges() {
		assertEquals(PossessionEndRules.Reason.VESSEL_FATAL, PossessionEndRules.reason(
				false, false, true, false, false, false, false));
	}

	@Test
	void nonfatalTerminationReasonsRemainDistinctAndNeverInvokeWrath() {
		assertEquals(PossessionEndRules.Reason.EXPIRED, PossessionEndRules.reason(
				true, true, true, true, true, true, false));
		assertEquals(PossessionEndRules.Reason.TARGET_UNAVAILABLE, PossessionEndRules.reason(
				true, false, true, true, true, true, true));
		assertEquals(PossessionEndRules.Reason.OWNER_INVALID, PossessionEndRules.reason(
				true, true, false, true, true, true, true));
		assertEquals(PossessionEndRules.Reason.PROTECTION_LOST, PossessionEndRules.reason(
				true, true, true, true, false, true, true));
		assertEquals(PossessionEndRules.Reason.SOURCE_LOST, PossessionEndRules.reason(
				true, true, true, true, true, false, true));
		assertEquals(PossessionEndRules.Reason.NONE, PossessionEndRules.reason(
				true, true, true, true, true, true, true));
	}

	@Test
	void divineWrathIsSevereButCannotKillTheReturnedControllerByItself() {
		assertEquals(35, PossessionEndRules.wrathEnergyDrain(100));
		assertEquals(25, PossessionEndRules.wrathEnergyDrain(40));
		assertEquals(200, PossessionEndRules.wrathEnergyDrain(40_000_000));
		assertEquals(12.0F, PossessionEndRules.wrathDamage(20.0F));
		assertEquals(1.0F, PossessionEndRules.wrathDamage(2.0F));
		assertEquals(0.0F, PossessionEndRules.wrathDamage(1.0F));
		assertEquals(200, PossessionEndRules.WRATH_TICKS);
	}
}

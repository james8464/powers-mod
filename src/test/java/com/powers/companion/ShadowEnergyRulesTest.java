package com.powers.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowEnergyRulesTest {
	@Test
	void linkRefillsNineHundredPerSecondInFiveTickPulses() {
		var result = ShadowEnergyRules.tick(new ShadowEnergyRules.EnergyFacts(
				0, true, false, false, false, false));
		assertEquals(225, result.energy());
		assertEquals(225, result.delta());
	}

	@Test
	void forceAndAmethystCounterplayClampAndSuppressActions() {
		assertTrue(ShadowEnergyRules.tick(new ShadowEnergyRules.EnergyFacts(
				1000, false, false, false, true, false)).actionsSuppressed());
		assertEquals(850, ShadowEnergyRules.tick(new ShadowEnergyRules.EnergyFacts(
				1000, false, false, false, true, false)).energy());
		assertEquals(925, ShadowEnergyRules.tick(new ShadowEnergyRules.EnergyFacts(
				1000, false, false, true, false, false)).energy());
		assertEquals(1080, ShadowEnergyRules.tick(new ShadowEnergyRules.EnergyFacts(
				1000, false, true, false, false, false)).energy());
	}

	@Test
	void testingRefillsButNeverExceedsTheDarknessMaximum() {
		var result = ShadowEnergyRules.tick(new ShadowEnergyRules.EnergyFacts(
				-50, true, true, false, true, true));
		assertEquals(1850, result.energy());
		assertEquals(false, result.actionsSuppressed());
	}
}

package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantHealingRulesTest {
	@Test
	void crouchHealingIncludesTheTwoBlockBoundaryButNothingBeyondIt() {
		assertTrue(PlantHealingRules.withinAura(0.0));
		assertTrue(PlantHealingRules.withinAura(4.0));
		assertFalse(PlantHealingRules.withinAura(Math.nextUp(4.0)));
		assertFalse(PlantHealingRules.withinAura(Double.NaN));
	}
}

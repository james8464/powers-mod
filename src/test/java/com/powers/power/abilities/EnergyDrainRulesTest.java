package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnergyDrainRulesTest {
	@Test
	void mobVitalityDrainScalesForModdedBossesButKeepsHardCaps() {
		assertEquals(30.0F, EnergyDrainRules.mobCompletionDamage(100.0F));
		assertEquals(1_200.0F, EnergyDrainRules.mobCompletionDamage(10_000.0F));
		assertEquals(2.0F, EnergyDrainRules.mobPulseDamage(100.0F));
		assertEquals(40.0F, EnergyDrainRules.mobPulseDamage(10_000.0F));
	}

	@Test
	void resistanceNeedsEightConsecutiveServerObservedTicks() {
		int progress = 0;
		for (int tick = 0; tick < 7; tick++) {
			progress = EnergyDrainRules.resistanceProgress(progress, true);
		}
		assertFalse(EnergyDrainRules.countered(progress));
		assertEquals(0, EnergyDrainRules.resistanceProgress(progress, false));
		assertTrue(EnergyDrainRules.countered(EnergyDrainRules.resistanceProgress(progress, true)));
		assertEquals(0x7A22A8, EnergyDrainRules.tetherStressColor(0));
		assertEquals(0xE8D7FF, EnergyDrainRules.tetherStressColor(EnergyDrainRules.RESISTANCE_TICKS));
	}

	@Test
	void mobConversionUsesOneHealthScaledAbsoluteBudget() {
		assertEquals(20, EnergyDrainRules.mobConversionCap(100.0F));
		assertEquals(120, EnergyDrainRules.mobConversionCap(10_000.0F));
		assertEquals(5, EnergyDrainRules.conversionGrant(5, 0, 20));
		assertEquals(3, EnergyDrainRules.conversionGrant(5, 17, 20));
		assertEquals(0, EnergyDrainRules.conversionGrant(120, 20, 20));
	}
}

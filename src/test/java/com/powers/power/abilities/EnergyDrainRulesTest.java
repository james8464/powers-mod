package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnergyDrainRulesTest {
	@Test
	void mobVitalityDrainScalesForModdedBossesButKeepsHardCaps() {
		assertEquals(30.0F, EnergyDrainRules.mobCompletionDamage(100.0F));
		assertEquals(1_200.0F, EnergyDrainRules.mobCompletionDamage(10_000.0F));
		assertEquals(2.0F, EnergyDrainRules.mobPulseDamage(100.0F));
		assertEquals(40.0F, EnergyDrainRules.mobPulseDamage(10_000.0F));
	}
}

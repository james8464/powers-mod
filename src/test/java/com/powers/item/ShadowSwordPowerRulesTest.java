package com.powers.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShadowSwordPowerRulesTest {
	@Test
	void swordAttacksRemainBossScaleButBounded() {
		assertEquals(90.0F, ShadowSwordPowerRules.singularityDamage(100.0F));
		assertEquals(300.0F, ShadowSwordPowerRules.singularityDamage(5000.0F));
		assertEquals(165.0F, ShadowSwordPowerRules.annihilationDamage(100.0F));
		assertEquals(500.0F, ShadowSwordPowerRules.annihilationDamage(5000.0F));
		assertEquals(80.0F, ShadowSwordPowerRules.oblivionDamage(100.0F));
		assertEquals(400.0F, ShadowSwordPowerRules.oblivionDamage(5000.0F));
		assertEquals(175.0F, ShadowSwordPowerRules.soulRequiemDamage(100.0F));
		assertEquals(750.0F, ShadowSwordPowerRules.soulRequiemDamage(5000.0F));
	}

	@Test
	void unlocksFormAFiveStageDarknessProgression() {
		assertEquals(3, ShadowSwordPowerRules.requiredRank("abyssal_singularity"));
		assertEquals(5, ShadowSwordPowerRules.requiredRank("oblivion_pulse"));
		assertEquals(7, ShadowSwordPowerRules.requiredRank("annihilation_beam"));
		assertEquals(9, ShadowSwordPowerRules.requiredRank("soul_requiem"));
		assertEquals(10, ShadowSwordPowerRules.requiredRank("nightfall_dominion"));
	}
}

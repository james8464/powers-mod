package com.powers.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShadowSwordPowerRulesTest {
	@Test
	void singularityAndBeamRemainBossScaleButBounded() {
		assertEquals(90.0F, ShadowSwordPowerRules.singularityDamage(100.0F));
		assertEquals(300.0F, ShadowSwordPowerRules.singularityDamage(5000.0F));
		assertEquals(165.0F, ShadowSwordPowerRules.annihilationDamage(100.0F));
		assertEquals(500.0F, ShadowSwordPowerRules.annihilationDamage(5000.0F));
	}

	@Test
	void unlocksFormAThreeStageDarknessProgression() {
		assertEquals(3, ShadowSwordPowerRules.requiredRank("abyssal_singularity"));
		assertEquals(7, ShadowSwordPowerRules.requiredRank("annihilation_beam"));
		assertEquals(10, ShadowSwordPowerRules.requiredRank("nightfall_dominion"));
	}
}

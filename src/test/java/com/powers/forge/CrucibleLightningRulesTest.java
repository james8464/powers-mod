package com.powers.forge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CrucibleLightningRulesTest {
	@Test
	void energyAndDamageScaleWithoutCooldown() {
		assertEquals(12, CrucibleLightningRules.energyCost(0));
		assertEquals(13, CrucibleLightningRules.energyCost(1));
		assertEquals(20, CrucibleLightningRules.energyCost(30));
		assertEquals(18.0F, CrucibleLightningRules.damage(0, false, false));
		assertEquals(30.0F, CrucibleLightningRules.damage(0, true, false));
		assertEquals(480.0F, CrucibleLightningRules.damage(30, true, false));
		assertEquals(0, CrucibleLightningRules.cooldownTicks());
	}

	@Test
	void damageCapsProtectPlayersAndServers() {
		assertEquals(120.0F, CrucibleLightningRules.capDamage(2_000.0F, true));
		assertEquals(1_200.0F, CrucibleLightningRules.capDamage(2_000.0F, false));
	}
}

package com.powers.power;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.world.damagesource.DamageTypes;

class PowerDamagePolicyTest {
	@Test
	void acceptsOnlyTheDedicatedPowerDamageKey() {
		assertTrue(PowerDamage.isPowerDamageKey(PowerDamage.POWER_MAGIC));
		assertFalse(PowerDamage.isPowerDamageKey(DamageTypes.MAGIC));
		assertFalse(PowerDamage.isPowerDamageKey(DamageTypes.INDIRECT_MAGIC));
		assertFalse(PowerDamage.isPowerDamageKey(DamageTypes.FREEZE));
	}
}

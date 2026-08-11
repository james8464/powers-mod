package com.powers.compat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThirdPartyCombatCompatibilityTest {
	@BeforeAll
	static void bootstrap() {
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
	}

	@Test
	void foreignProjectileDamageAndEffectUseSafeDefaults() {
		assertEquals(CombatCompatibilityDisposition.FOREIGN_UNCHANGED,
				ThirdPartyCombatCompatibility.projectile(false));
		org.junit.jupiter.api.Assertions.assertFalse(com.powers.power.PowerDamage.isPowerDamage(null));
		var infinite = com.powers.PowerStatusEffects.hidden(
				net.minecraft.world.effect.MobEffects.INVISIBILITY, -1, 255, false, false);
		assertEquals(-1, infinite.getDuration());
	}

	@Test
	void ownedSignalsRemainExplicit() {
		assertEquals(CombatCompatibilityDisposition.POWERS_OWNED,
				ThirdPartyCombatCompatibility.projectile(true));
	}
}

package com.powers.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThirdPartyCombatCompatibilityTest {
	@Test
	void foreignProjectileDamageAndEffectUseSafeDefaults() {
		assertEquals(CombatCompatibilityDisposition.FOREIGN_UNCHANGED,
				ThirdPartyCombatCompatibility.projectile(false));
		assertEquals(CombatCompatibilityDisposition.FOREIGN_UNCHANGED,
				ThirdPartyCombatCompatibility.damage(null));
		assertEquals(0, ThirdPartyCombatCompatibility.effectDuration(-99));
		assertEquals(20 * 60 * 60, ThirdPartyCombatCompatibility.effectDuration(Integer.MAX_VALUE));
	}

	@Test
	void ownedSignalsRemainExplicit() {
		assertEquals(CombatCompatibilityDisposition.POWERS_OWNED,
				ThirdPartyCombatCompatibility.projectile(true));
		assertEquals(CombatCompatibilityDisposition.POWERS_OWNED,
				ThirdPartyCombatCompatibility.damage("powers:power_magic"));
	}
}

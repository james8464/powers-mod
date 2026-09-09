package com.powers.power.abilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IceLifecycleRulesTest {
	@Test
	void brittleArmourIsFiniteRefreshableAndFireBreakable() {
		assertEquals(100, IceLifecycleRules.brittleDurationTicks());
		assertEquals(-0.25, IceLifecycleRules.armourMultiplier());
		assertEquals(140L, IceLifecycleRules.refreshExpiry(80L, 40L));
		assertEquals(180L, IceLifecycleRules.refreshExpiry(180L, 40L));
		assertFalse(IceLifecycleRules.expired(139L, 140L));
		assertTrue(IceLifecycleRules.expired(140L, 140L));
		assertTrue(IceLifecycleRules.fireBreaksBrittleArmour(true));
		assertFalse(IceLifecycleRules.fireBreaksBrittleArmour(false));
	}
}

package com.powers.force;

import org.junit.jupiter.api.Test;

import static com.powers.force.LivingForceKind.DARKNESS;
import static com.powers.force.LivingForceKind.PURE_LIGHT;
import static com.powers.force.LivingForceRules.Affinity.NONE;
import static com.powers.force.LivingForceRules.Affinity.REFILL;
import static com.powers.force.LivingForceRules.Affinity.WITHER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivingForceRulesTest {
	@Test
	void darknessAffinityWithersOrdinaryEntitiesAndRefillsDarknessEntities() {
		assertEquals(WITHER, LivingForceRules.affinity(false, DARKNESS));
		assertEquals(REFILL, LivingForceRules.affinity(true, DARKNESS));
		assertEquals(NONE, LivingForceRules.affinity(false, PURE_LIGHT));
		assertEquals(NONE, LivingForceRules.affinity(true, PURE_LIGHT));
	}

	@Test
	void onlyOpposedForcesClash() {
		assertTrue(LivingForceRules.opposes(DARKNESS, PURE_LIGHT));
		assertTrue(LivingForceRules.opposes(PURE_LIGHT, DARKNESS));
		assertFalse(LivingForceRules.opposes(DARKNESS, DARKNESS));
		assertFalse(LivingForceRules.opposes(PURE_LIGHT, PURE_LIGHT));
	}

	@Test
	void clashDamageFallsQuadraticallyToZeroAtTheBoundary() {
		assertEquals(100.0, LivingForceRules.clashDamage(0.0, 48.0, 100.0), 0.0001);
		assertEquals(25.0, LivingForceRules.clashDamage(24.0, 48.0, 100.0), 0.0001);
		assertEquals(0.0, LivingForceRules.clashDamage(48.0, 48.0, 100.0), 0.0001);
		assertEquals(0.0, LivingForceRules.clashDamage(60.0, 48.0, 100.0), 0.0001);
	}

	@Test
	void sphereBoundaryIsInclusiveWithoutLeakingIntoCubeCorners() {
		assertTrue(LivingForceRules.insideSphere(3, 4, 0, 5));
		assertTrue(LivingForceRules.insideSphere(5, 0, 0, 5));
		assertFalse(LivingForceRules.insideSphere(5, 1, 0, 5));
		assertFalse(LivingForceRules.insideSphere(4, 4, 4, 5));
	}
}

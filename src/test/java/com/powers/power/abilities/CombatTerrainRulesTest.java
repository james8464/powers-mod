package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatTerrainRulesTest {
	@Test
	void everyOffensiveTierLeavesAVisibleButBoundedScar() {
		assertTrue(CombatTerrainRules.craterBudget(0) > 0);
		assertTrue(CombatTerrainRules.thunderclapBudget(0) > 0);
		assertTrue(CombatTerrainRules.rayBudget(0) > 0);
		assertTrue(CombatTerrainRules.craterBudget(10) > CombatTerrainRules.craterBudget(0));
		assertTrue(CombatTerrainRules.thunderclapBudget(10) > CombatTerrainRules.thunderclapBudget(0));
		assertTrue(CombatTerrainRules.rayBudget(10) > CombatTerrainRules.rayBudget(0));
		assertEquals(96, CombatTerrainRules.craterBudget(10));
		assertEquals(48, CombatTerrainRules.thunderclapBudget(10));
		assertEquals(8, CombatTerrainRules.rayBudget(10));
	}

	@Test
	void rankInputsAreClampedBeforeWorkBudgetsAreAllocated() {
		assertEquals(CombatTerrainRules.craterBudget(0), CombatTerrainRules.craterBudget(-50));
		assertEquals(CombatTerrainRules.rayBudget(10), CombatTerrainRules.rayBudget(99));
	}

	@Test
	void serverMinimumAndMaximumRemainBounded() {
		assertEquals(4, CombatTerrainRules.effectiveTier(2, 4));
		assertEquals(10, CombatTerrainRules.effectiveTier(99, -1));
		assertEquals(12, CombatTerrainRules.cappedBudget(96, 12));
		assertEquals(1, CombatTerrainRules.cappedBudget(96, -20));
	}
}

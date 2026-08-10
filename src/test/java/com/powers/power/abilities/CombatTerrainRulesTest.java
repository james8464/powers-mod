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
}

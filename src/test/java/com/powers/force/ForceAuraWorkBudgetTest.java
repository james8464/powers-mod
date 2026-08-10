package com.powers.force;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForceAuraWorkBudgetTest {
	@Test
	void globalAndPerPlayerInspectionLimitsAreHardCaps() {
		ForceAuraWorkBudget budget = new ForceAuraWorkBudget(4096, 512);
		assertEquals(512, budget.allowanceForPlayer());
		budget.recordInspections(510);
		assertEquals(512, budget.allowanceForPlayer());
		budget.recordInspections(4000);
		assertEquals(0, budget.allowanceForPlayer());
		assertFalse(budget.hasWork());
	}

	@Test
	void negativeAndOversizedReportsCannotBreakAccounting() {
		ForceAuraWorkBudget budget = new ForceAuraWorkBudget(4, 3);
		budget.recordInspections(-100);
		assertEquals(3, budget.allowanceForPlayer());
		budget.recordInspections(99);
		assertEquals(4, budget.inspected());
		assertFalse(budget.hasWork());
		assertTrue(new ForceAuraWorkBudget(1, 1).hasWork());
	}
}

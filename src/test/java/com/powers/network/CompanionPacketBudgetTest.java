package com.powers.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionPacketBudgetTest {
	@Test
	void ordinaryPacketsHaveAHardPerTickLimitThatResetsNextTick() {
		CompanionPacketBudget budget = new CompanionPacketBudget(2);
		assertTrue(budget.claim(10));
		assertTrue(budget.claim(10));
		assertFalse(budget.claim(10));
		assertTrue(budget.claim(11));
	}

	@Test
	void backwardsTickMovementAlsoStartsAFreshWindow() {
		CompanionPacketBudget budget = new CompanionPacketBudget(1);
		assertTrue(budget.claim(100));
		assertFalse(budget.claim(100));
		assertTrue(budget.claim(0));
	}
}

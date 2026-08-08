package com.powers.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParticleBudgetTest {
	@Test
	void capsOneTickAndResetsOnTheNext() {
		ParticleBudget budget = new ParticleBudget(10);
		assertEquals(7, budget.claim(100, 7));
		assertEquals(3, budget.claim(100, 9));
		assertEquals(0, budget.claim(100, 1));
		assertEquals(8, budget.claim(101, 8));
	}

	@Test
	void rejectsNegativeRequestsAndUsesAtLeastOneSlot() {
		ParticleBudget budget = new ParticleBudget(0);
		assertEquals(0, budget.claim(1, -3));
		assertEquals(1, budget.claim(1, 4));
	}
}

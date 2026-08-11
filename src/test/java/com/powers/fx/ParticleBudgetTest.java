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

	@Test
	void reducesDenseBurstsOnlyForViewersInsideTheFirstPersonClarityRadius() {
		assertEquals(5, ParticleBudget.viewerCount(20, 4.0));
		assertEquals(1, ParticleBudget.viewerCount(2, 15.9));
		assertEquals(20, ParticleBudget.viewerCount(20, 16.01));
		assertEquals(0, ParticleBudget.viewerCount(0, 0.0));
	}

	@Test
	void nearCameraForwardConePreservesTheReticle() {
		assertEquals(1, ParticleBudget.viewerCount(40, 1.0, 0.95));
		assertEquals(10, ParticleBudget.viewerCount(40, 1.0, -0.2));
		assertEquals(10, ParticleBudget.viewerCount(40, 9.0, 0.95));
		assertEquals(40, ParticleBudget.viewerCount(40, 25.0, 1.0));
	}
}

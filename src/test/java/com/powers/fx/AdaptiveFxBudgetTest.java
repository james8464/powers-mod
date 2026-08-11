package com.powers.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdaptiveFxBudgetTest {
	@Test
	void pressureDegradesImmediatelyButRecoveryRequiresAStableWindow() {
		AdaptiveFxBudget budget = new AdaptiveFxBudget(4);
		assertEquals(1.0, budget.update(30.0));
		assertEquals(0.5, budget.update(44.0));
		assertEquals(0.25, budget.update(51.0));
		assertEquals(0.25, budget.update(30.0));
		assertEquals(0.25, budget.update(30.0));
		assertEquals(0.25, budget.update(30.0));
		assertEquals(0.5, budget.update(30.0));
		for (int tick = 0; tick < 4; tick++) budget.update(30.0);
		assertEquals(1.0, budget.scale());
	}

	@Test
	void scaledGeometryRetainsAReadableMinimum() {
		assertEquals(6, AdaptiveFxBudget.scaleCount(24, 0.25, 6));
		assertEquals(24, AdaptiveFxBudget.scaleCount(24, 1.0, 6));
		assertEquals(0, AdaptiveFxBudget.scaleCount(0, 0.25, 6));
	}
}

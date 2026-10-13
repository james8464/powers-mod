package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DimensionalStabilizationRulesTest {
	@Test
	void onlyLoadedLocalBodiesCanBeStabilized() {
		assertTrue(DimensionalStabilizationRules.localBody(true, true));
		assertFalse(DimensionalStabilizationRules.localBody(false, true));
		assertFalse(DimensionalStabilizationRules.localBody(true, false));
	}

	@Test
	void existingGateMustRemainLoadedLocalAndLegalAtBothEnds() {
		assertTrue(DimensionalStabilizationRules.existingRoute(
				true, true, true, true, true, true, true).allowed());
		assertEquals(DimensionalStabilizationRules.Refusal.MISSING,
				DimensionalStabilizationRules.existingRoute(
						false, true, true, true, true, true, true).refusal());
		assertEquals(DimensionalStabilizationRules.Refusal.UNLOADED,
				DimensionalStabilizationRules.existingRoute(
						true, true, false, true, true, true, true).refusal());
		assertEquals(DimensionalStabilizationRules.Refusal.CROSS_DIMENSION,
				DimensionalStabilizationRules.existingRoute(
						true, true, true, false, true, true, true).refusal());
		assertEquals(DimensionalStabilizationRules.Refusal.PROTECTED,
				DimensionalStabilizationRules.existingRoute(
						true, true, true, true, true, true, false).refusal());
		assertEquals(DimensionalStabilizationRules.Refusal.OUT_OF_RANGE,
				DimensionalStabilizationRules.existingRoute(
						true, true, true, true, false, true, true).refusal());
	}

	@Test
	void extensionIsFiniteAndNeverShortensAnExistingLease() {
		assertEquals(1_600L, DimensionalStabilizationRules.extendedDeadline(
				1_000L, 1_200L, 400));
		assertEquals(Long.MAX_VALUE, DimensionalStabilizationRules.extendedDeadline(
				1_000L, Long.MAX_VALUE, Integer.MAX_VALUE));
		assertEquals(1_001L, DimensionalStabilizationRules.extendedDeadline(
				1_000L, 0L, 0));
	}
}

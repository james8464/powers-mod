package com.powers.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Covers irreversible lock timing and overflow-safe preview geometry. */
class CelestialRuinStagingRulesTest {
	@Test
	void cancellationEndsExactlyWhenChunkCommitPreparationBegins() {
		assertTrue(CelestialRuinStagingRules.mayCancel(1_200, false));
		assertTrue(CelestialRuinStagingRules.mayCancel(101, false));
		assertFalse(CelestialRuinStagingRules.mayCancel(100, false));
		assertFalse(CelestialRuinStagingRules.mayCancel(1_200, true));
	}

	@Test
	void previewChunkFootprintsAreFiniteAndConservative() {
		assertEquals(289, CelestialRuinStagingRules.squareChunkFootprint(120));
		assertEquals(564_001, CelestialRuinStagingRules.squareChunkFootprint(6_000));
		assertEquals(Integer.MAX_VALUE, CelestialRuinStagingRules.squareChunkFootprint(Integer.MAX_VALUE));
	}
}

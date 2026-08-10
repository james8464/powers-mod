package com.powers.magic.runtime;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CastScalingContextTest {
	@Test
	void bindingIsNestedAndAlwaysRestored() {
		CastAdjustment outer = new CastAdjustment(true, 1.2, 0.8, 1.1, List.of());
		CastAdjustment inner = new CastAdjustment(true, 0.5, 0.6, 0.7, List.of());

		assertEquals(1.0, CastScalingContext.current().potencyMultiplier());
		CastScalingContext.with(outer, () -> {
			assertEquals(1.2, CastScalingContext.current().potencyMultiplier());
			CastScalingContext.with(inner,
					() -> assertEquals(0.5, CastScalingContext.current().potencyMultiplier()));
			assertEquals(1.2, CastScalingContext.current().potencyMultiplier());
		});
		assertEquals(1.0, CastScalingContext.current().potencyMultiplier());
	}

	@Test
	void artifactSourceSurvivesNestedInteractionAdjustmentAndRestores() {
		CastAdjustment adjusted = new CastAdjustment(true, 1.2, 0.8, 1.1, List.of());

		assertEquals(CastSource.INNATE, CastScalingContext.currentSource());
		CastScalingContext.withSource(CastSource.ARTIFACT, () -> {
			assertEquals(CastSource.ARTIFACT, CastScalingContext.currentSource());
			assertFalse(CastScalingContext.currentSource().appliesPlayerRank(true));
			CastScalingContext.with(adjusted, () ->
					assertEquals(CastSource.ARTIFACT, CastScalingContext.currentSource()));
		});
		assertEquals(CastSource.INNATE, CastScalingContext.currentSource());
		assertTrue(CastScalingContext.currentSource().appliesPlayerRank(true));
	}

	@Test
	void onlyInnateCastSourcesCanApplyAnOptedInAbilityRank() {
		assertTrue(CastSource.INNATE.appliesPlayerRank(true));
		assertFalse(CastSource.INNATE.appliesPlayerRank(false));
		assertFalse(CastSource.ARTIFACT.appliesPlayerRank(true));
		assertFalse(CastSource.CRYSTAL.appliesPlayerRank(true));
		assertFalse(CastSource.SPELL.appliesPlayerRank(true));
	}
}

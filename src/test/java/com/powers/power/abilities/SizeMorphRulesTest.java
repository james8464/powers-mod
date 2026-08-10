package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SizeMorphRulesTest {
	@Test
	void selectableScalesIncludeSymmetricShrinkNormalAndGrowthSteps() {
		assertEquals(List.of(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0),
				SizeMorphRules.scales());
		assertEquals(3, SizeMorphRules.normalOption());
		assertTrue(SizeMorphRules.isValidOption(0));
		assertTrue(SizeMorphRules.isValidOption(7));
		assertFalse(SizeMorphRules.isValidOption(-1));
		assertFalse(SizeMorphRules.isValidOption(8));
		assertEquals(0.25, SizeMorphRules.scale(0));
		assertEquals(2.0, SizeMorphRules.scale(7));
		assertThrows(IllegalArgumentException.class, () -> SizeMorphRules.scale(8));
	}

	@Test
	void ongoingDrainIsProportionalToDistanceFromNormalScale() {
		assertEquals(0, SizeMorphRules.energyDrainPerSecond(1.0));
		assertEquals(1, SizeMorphRules.energyDrainPerSecond(0.75));
		assertEquals(1, SizeMorphRules.energyDrainPerSecond(1.25));
		assertEquals(2, SizeMorphRules.energyDrainPerSecond(0.5));
		assertEquals(2, SizeMorphRules.energyDrainPerSecond(1.5));
		assertEquals(4, SizeMorphRules.energyDrainPerSecond(2.0));
	}
}

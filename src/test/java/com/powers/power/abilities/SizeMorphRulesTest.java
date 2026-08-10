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
		assertEquals(List.of(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0,
				0.125, 2.5, 3.0, 4.0),
				SizeMorphRules.scales());
		assertEquals(3, SizeMorphRules.normalOption());
		assertTrue(SizeMorphRules.isValidOption(0));
		assertTrue(SizeMorphRules.isValidOption(11));
		assertFalse(SizeMorphRules.isValidOption(-1));
		assertFalse(SizeMorphRules.isValidOption(12));
		assertEquals(0.25, SizeMorphRules.scale(0));
		assertEquals(2.0, SizeMorphRules.scale(7));
		assertEquals(0.125, SizeMorphRules.scale(8));
		assertEquals(4.0, SizeMorphRules.scale(11));
		assertThrows(IllegalArgumentException.class, () -> SizeMorphRules.scale(12));
	}

	@Test
	void extremeFormsUnlockAtAuthoredRankBreakpointsWithoutChangingSavedIndices() {
		for (int option = 0; option <= 7; option++) {
			assertEquals(0, SizeMorphRules.minimumRank(option));
		}
		assertEquals(6, SizeMorphRules.minimumRank(8));
		assertEquals(4, SizeMorphRules.minimumRank(9));
		assertEquals(7, SizeMorphRules.minimumRank(10));
		assertEquals(10, SizeMorphRules.minimumRank(11));
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

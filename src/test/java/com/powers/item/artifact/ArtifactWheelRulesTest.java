package com.powers.item.artifact;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArtifactWheelRulesTest {
	@Test
	void eightSegmentsStartAtNorthAndAdvanceClockwise() {
		assertEquals(0, ArtifactWheelRules.targetAt(100, 100, 100, 50));
		assertEquals(2, ArtifactWheelRules.targetAt(100, 100, 150, 100));
		assertEquals(4, ArtifactWheelRules.targetAt(100, 100, 100, 150));
		assertEquals(6, ArtifactWheelRules.targetAt(100, 100, 50, 100));
	}

	@Test
	void centerAndOutsideAreDistinctFromFavouriteSlots() {
		assertEquals(ArtifactWheelRules.CENTER, ArtifactWheelRules.targetAt(100, 100, 110, 100));
		assertEquals(ArtifactWheelRules.NONE, ArtifactWheelRules.targetAt(100, 100, 190, 100));
	}

	@Test
	void topRowAndKeypadDigitsSelectTheMatchingFavourite() {
		assertEquals(0, ArtifactWheelRules.numberSlot(49));
		assertEquals(7, ArtifactWheelRules.numberSlot(56));
		assertEquals(0, ArtifactWheelRules.numberSlot(321));
		assertEquals(7, ArtifactWheelRules.numberSlot(328));
		assertEquals(ArtifactWheelRules.NONE, ArtifactWheelRules.numberSlot(57));
		assertEquals(true, ArtifactWheelRules.isShift(340));
		assertEquals(true, ArtifactWheelRules.isShift(344));
		assertEquals(3, ArtifactWheelRules.releasedSelection(340, 3));
		assertEquals(ArtifactWheelRules.NONE, ArtifactWheelRules.releasedSelection(65, 3));
		assertEquals(ArtifactWheelRules.NONE,
				ArtifactWheelRules.releasedSelection(340, ArtifactWheelRules.CENTER));
	}
}

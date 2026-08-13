package com.powers.item.artifact;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	void liveSegmentStatusKeepsEveryCombatDecisionVisible() {
		ArtifactWheelRules.SegmentStatus ready = ArtifactWheelRules.segmentStatus(
				12, 20, 0, 80, false, false, -1);
		assertEquals(new ArtifactWheelRules.SegmentStatus(12, true, 0, false, false, -1), ready);

		ArtifactWheelRules.SegmentStatus cooling = ArtifactWheelRules.segmentStatus(
				30, 12, 25, 40, true, false, 6);
		assertEquals(new ArtifactWheelRules.SegmentStatus(30, false, 5, true, false, 6), cooling);

		ArtifactWheelRules.SegmentStatus locked = ArtifactWheelRules.segmentStatus(
				4, -1, 200, 20, false, true, -1);
		assertEquals(new ArtifactWheelRules.SegmentStatus(4, false, 8, false, true, -1), locked);
	}

	@Test
	void authenticatedCooldownSnapshotCountsDownWhileWheelRemainsOpen() {
		assertEquals(80, ArtifactWheelRules.remainingCooldown(100, 20));
		assertEquals(0, ArtifactWheelRules.remainingCooldown(100, 140));
		assertEquals(0, ArtifactWheelRules.remainingCooldown(-1, 5));
	}

	@Test
	void releaseToCastIsExplicitlyOptInWhileSafeReleaseOnlySelects() {
		assertEquals(ArtifactWheelRules.ReleaseAction.SELECT,
				ArtifactWheelRules.releaseAction(false, 340, 2));
		assertEquals(ArtifactWheelRules.ReleaseAction.CAST,
				ArtifactWheelRules.releaseAction(true, 340, 2));
		assertEquals(ArtifactWheelRules.ReleaseAction.NONE,
				ArtifactWheelRules.releaseAction(true, 65, 2));
	}

	@Test
	void responsiveLayoutKeepsAdjacentSegmentLabelsDisjoint() {
		ArtifactWheelRules.Layout standard = ArtifactWheelRules.layout(640, 360);
		assertEquals(120, standard.outerRadius());
		assertEquals(114, standard.glyphRadius());
		assertEquals(82, standard.verticalGlyphRadius());
		assertTrue(standard.nameWidth() >= 80);
		assertTrue(ArtifactWheelRules.adjacentGlyphDistance(standard)
				> standard.nameWidth());

		ArtifactWheelRules.Layout compact = ArtifactWheelRules.layout(320, 240);
		assertEquals(95, compact.outerRadius());
		assertEquals(89, compact.glyphRadius());
		assertEquals(57, compact.verticalGlyphRadius());
		assertEquals(false, compact.showSegmentNames());

		ArtifactWheelRules.Layout highGuiScale = ArtifactWheelRules.layout(320, 180);
		assertEquals(false, highGuiScale.showSegmentNames());
		assertEquals(true, standard.showSegmentNames());
	}

	@Test
	void compactLabelsRemainRecognisableInsteadOfSilentlyClipping() {
		assertEquals("Fireball", ArtifactWheelRules.compactLabel("Fireball", 10));
		assertEquals("Lightning", ArtifactWheelRules.compactLabel("Lightning Strike", 10));
		assertEquals("Hollowed", ArtifactWheelRules.compactLabel("Call the Hollowed", 10));
		assertEquals("Nightfall", ArtifactWheelRules.compactLabel("Nightfall Dominion", 10));
	}
}

package com.powers.progression;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankPresentationTest {
	@Test
	void mazeDescribesRealTransformationsInsteadOfRetiredPercentPerks() {
		RankNode motion = new RankNode("motion", 3, "motion", "Wind-Touched",
				List.of(), false, List.of());
		RankNode legacy = new RankNode("legacy_4", 4, "origin", "Adept",
				List.of(), true, List.of());

		assertEquals("Second Step · kinetic movement transformation",
				RankPresentation.summary(motion));
		assertTrue(RankPresentation.summary(legacy).contains("Innate tier 4"));
		assertTrue(!RankPresentation.summary(legacy).contains("%"));
	}

	@Test
	void readableScaleFitsLongLabelsWithoutVanishing() {
		assertEquals(1.0F, RankPresentation.readableScale(70, 60));
		assertEquals(0.7F, RankPresentation.readableScale(70, 100));
		assertEquals(0.55F, RankPresentation.readableScale(40, 200));
	}
}

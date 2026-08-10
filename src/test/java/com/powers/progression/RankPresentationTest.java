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
}

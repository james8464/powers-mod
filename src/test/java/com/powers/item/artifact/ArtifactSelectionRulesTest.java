package com.powers.item.artifact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArtifactSelectionRulesTest {
	@Test
	void selectionMustBelongToTheHeldAlignmentAndMeetRank() {
		var dark = ArtifactActionCatalogue.find(
				ArtifactAlignment.DARKNESS, "unique/nightfall_dominion");
		assertTrue(ArtifactSelectionRules.maySelect(dark, ArtifactAlignment.DARKNESS, 10));
		assertFalse(ArtifactSelectionRules.maySelect(dark, ArtifactAlignment.LIGHT, 10));
		assertFalse(ArtifactSelectionRules.maySelect(dark, ArtifactAlignment.DARKNESS, 9));
		assertFalse(ArtifactSelectionRules.maySelect(null, ArtifactAlignment.DARKNESS, 10));
	}

	@Test
	void cyclingWrapsAndSkipsActionsAboveTheHeldArtifactsRank() {
		var actions = ArtifactActionCatalogue.forAlignment(ArtifactAlignment.LIGHT);
		String lastRankOne = actions.stream().filter(action -> action.requiredRank() <= 1)
				.reduce((first, second) -> second).orElseThrow().key();
		assertEquals(actions.getFirst().key(), ArtifactSelectionRules.cycleKey(
				actions, lastRankOne, ArtifactAlignment.LIGHT, 1, 1));
		assertEquals(lastRankOne, ArtifactSelectionRules.cycleKey(
				actions, actions.getFirst().key(), ArtifactAlignment.LIGHT, 1, -1));
		assertEquals(actions.getFirst().key(), ArtifactSelectionRules.cycleKey(
				actions, "malformed", ArtifactAlignment.LIGHT, 1, 1));
	}
}

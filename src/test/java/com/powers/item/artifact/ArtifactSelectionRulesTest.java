package com.powers.item.artifact;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArtifactSelectionRulesTest {
	@Test
	void selectionMustBelongToTheHeldAlignmentAndMeetRank() {
		var dark = ArtifactActionCatalogue.find(ArtifactAlignment.DARKNESS, "dominion/black_decree");
		assertTrue(ArtifactSelectionRules.maySelect(dark, ArtifactAlignment.DARKNESS, 8));
		assertFalse(ArtifactSelectionRules.maySelect(dark, ArtifactAlignment.LIGHT, 10));
		assertFalse(ArtifactSelectionRules.maySelect(dark, ArtifactAlignment.DARKNESS, 7));
		assertFalse(ArtifactSelectionRules.maySelect(null, ArtifactAlignment.DARKNESS, 10));
	}
}

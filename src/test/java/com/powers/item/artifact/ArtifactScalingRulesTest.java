package com.powers.item.artifact;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ArtifactScalingRulesTest {
	@Test
	void maxDarknessApotheosisIsInsaneAndStrongerThanMaximumLight() {
		var shadow = ArtifactScalingRules.profile(ArtifactAlignment.DARKNESS, 10);
		var partisan = ArtifactScalingRules.profile(ArtifactAlignment.LIGHT, 10);
		org.junit.jupiter.api.Assertions.assertTrue(shadow.potency() >= 6.0);
		org.junit.jupiter.api.Assertions.assertTrue(shadow.range() >= 2.0);
		org.junit.jupiter.api.Assertions.assertTrue(shadow.duration() >= 2.0);
		org.junit.jupiter.api.Assertions.assertTrue(shadow.potency() > partisan.potency());
		org.junit.jupiter.api.Assertions.assertTrue(shadow.apotheosis());
	}

	@Test
	void artifactScalingIsDiscreteRatherThanLeakingEveryInnateRank() {
		var rankZero = ArtifactScalingRules.profile(ArtifactAlignment.DARKNESS, 0);
		var rankNine = ArtifactScalingRules.profile(ArtifactAlignment.DARKNESS, 9);
		org.junit.jupiter.api.Assertions.assertEquals(rankZero, rankNine);
		assertFalse(rankNine.apotheosis());
	}
}

package com.powers.item.artifact;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ArtifactCooldownRulesTest {
	@Test
	void darknessRankTenRemovesEveryArtifactCooldown() {
		assertEquals(200, ArtifactCooldownRules.cooldownTicks(ArtifactAlignment.DARKNESS, 9, 200));
		assertEquals(0, ArtifactCooldownRules.cooldownTicks(ArtifactAlignment.DARKNESS, 10, 200));
	}

	@Test
	void lightRankTenGetsTheSaferSixtyPercentReduction() {
		assertEquals(200, ArtifactCooldownRules.cooldownTicks(ArtifactAlignment.LIGHT, 9, 200));
		assertEquals(80, ArtifactCooldownRules.cooldownTicks(ArtifactAlignment.LIGHT, 10, 200));
	}
}

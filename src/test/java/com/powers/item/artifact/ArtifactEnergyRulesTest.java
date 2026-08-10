package com.powers.item.artifact;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArtifactEnergyRulesTest {
	@Test
	void rankTenRegenerationRulesRemainExact() {
		assertEquals(395, ArtifactEnergyRules.regenerationPerSecond(
				ArtifactAlignment.DARKNESS, 9));
		assertEquals(900, ArtifactEnergyRules.regenerationPerSecond(
				ArtifactAlignment.DARKNESS, 10));
		assertEquals(175, ArtifactEnergyRules.regenerationPerSecond(
				ArtifactAlignment.LIGHT, 9));
		assertEquals(300, ArtifactEnergyRules.regenerationPerSecond(
				ArtifactAlignment.LIGHT, 10));
	}
}

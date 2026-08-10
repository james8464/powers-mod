package com.powers.power.artifact;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ArtifactCovenantRulesTest {
	@Test
	void sharesHalfOfRealDamageWithoutCreatingOrOverhealingDamage() {
		assertEquals(0.0F, ArtifactCovenantRules.sharedDamage(-1.0F));
		assertEquals(0.0F, ArtifactCovenantRules.sharedDamage(0.0F));
		assertEquals(3.75F, ArtifactCovenantRules.sharedDamage(7.5F));
	}
}

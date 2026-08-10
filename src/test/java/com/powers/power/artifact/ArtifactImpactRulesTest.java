package com.powers.power.artifact;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactImpactRulesTest {
	@Test
	void damageAndForcedMovementAreIndependentProtectionDecisions() {
		ArtifactImpactRules.Decision damageOnly = ArtifactImpactRules.decide(
				true, false, true, false, false);
		assertTrue(damageOnly.damage());
		assertFalse(damageOnly.move());

		ArtifactImpactRules.Decision movementOnly = ArtifactImpactRules.decide(
				true, false, false, true, false);
		assertFalse(movementOnly.damage());
		assertTrue(movementOnly.move());
	}

	@Test
	void amethystAndKineticWardsBlockTheirRespectiveOutcomes() {
		ArtifactImpactRules.Decision dampened = ArtifactImpactRules.decide(
				true, true, true, true, false);
		assertFalse(dampened.damage());
		assertFalse(dampened.move());

		ArtifactImpactRules.Decision warded = ArtifactImpactRules.decide(
				true, false, true, true, true);
		assertTrue(warded.damage());
		assertFalse(warded.move());
	}
}

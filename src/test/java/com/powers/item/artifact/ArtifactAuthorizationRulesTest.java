package com.powers.item.artifact;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArtifactAuthorizationRulesTest {
	@Test
	void opposedArtifactsRejectTheWrongPath() {
		assertTrue(ArtifactAuthorizationRules.mayUse(ArtifactAlignment.DARKNESS, true));
		assertFalse(ArtifactAuthorizationRules.mayUse(ArtifactAlignment.DARKNESS, false));
		assertTrue(ArtifactAuthorizationRules.mayUse(ArtifactAlignment.LIGHT, false));
		assertFalse(ArtifactAuthorizationRules.mayUse(ArtifactAlignment.LIGHT, true));
	}

	@Test
	void energyIsAlwaysRequiredEvenAtAscendance() {
		assertTrue(ArtifactAuthorizationRules.requiresEnergy(ArtifactAlignment.DARKNESS, 10));
		assertTrue(ArtifactAuthorizationRules.requiresEnergy(ArtifactAlignment.LIGHT, 10));
	}
}

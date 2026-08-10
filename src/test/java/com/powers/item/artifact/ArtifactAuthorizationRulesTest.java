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
		assertFalse(ArtifactAuthorizationRules.mayUse(null, false));
	}

	@Test
	void energyIsAlwaysRequiredEvenAtAscendance() {
		assertTrue(ArtifactAuthorizationRules.requiresEnergy(ArtifactAlignment.DARKNESS, 10));
		assertTrue(ArtifactAuthorizationRules.requiresEnergy(ArtifactAlignment.LIGHT, 10));
	}

	@Test
	void sustainedEffectsRequireLiveMagicAndPhysicalAuthorization() {
		assertTrue(ArtifactAuthorizationRules.maySustain(true, true, true));
		assertFalse(ArtifactAuthorizationRules.maySustain(false, true, true));
		assertFalse(ArtifactAuthorizationRules.maySustain(true, false, true));
		assertFalse(ArtifactAuthorizationRules.maySustain(true, true, false));
	}
}

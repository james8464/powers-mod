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
		assertTrue(ArtifactAuthorizationRules.requiresEnergy(ArtifactAlignment.DARKNESS, 0));
		assertFalse(ArtifactAuthorizationRules.requiresEnergy(ArtifactAlignment.DARKNESS, -1));
		assertFalse(ArtifactAuthorizationRules.requiresEnergy(null, 10));
	}

	@Test
	void sustainedEffectsRequireLiveMagicAndPhysicalAuthorization() {
		assertTrue(ArtifactAuthorizationRules.maySustain(true, true, true));
		assertFalse(ArtifactAuthorizationRules.maySustain(false, true, true));
		assertFalse(ArtifactAuthorizationRules.maySustain(true, false, true));
		assertFalse(ArtifactAuthorizationRules.maySustain(true, true, false));
	}

	@Test
	void inventoryAndHeldOwnershipAreDistinctAndFailClosed() {
		assertTrue(ArtifactAuthorizationRules.mayOwn(true, true));
		assertFalse(ArtifactAuthorizationRules.mayOwn(false, true));
		assertFalse(ArtifactAuthorizationRules.mayOwn(true, false));

		assertTrue(ArtifactAuthorizationRules.maySustain(true, true, true, true, true));
		assertFalse(ArtifactAuthorizationRules.maySustain(true, true, false, true, true));
		assertTrue(ArtifactAuthorizationRules.maySustain(true, true, false, true, false));
		assertFalse(ArtifactAuthorizationRules.maySustain(false, true, true, true, false));
	}
}

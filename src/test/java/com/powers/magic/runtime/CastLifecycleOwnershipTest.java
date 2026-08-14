package com.powers.magic.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CastLifecycleOwnershipTest {
	@Test
	void innateAndArtifactChannelsRetainOnlyTheirActualOwnership() {
		assertTrue(CastLifecycleOwnership.mayContinue(
				CastSource.INNATE, true, false, false));
		assertFalse(CastLifecycleOwnership.mayContinue(
				CastSource.INNATE, false, true, true));
		assertTrue(CastLifecycleOwnership.mayContinue(
				CastSource.ARTIFACT, false, true, true));
		assertFalse(CastLifecycleOwnership.mayContinue(
				CastSource.ARTIFACT, true, true, false));
		assertFalse(CastLifecycleOwnership.mayContinue(
				CastSource.ARTIFACT, true, false, true));
	}
}

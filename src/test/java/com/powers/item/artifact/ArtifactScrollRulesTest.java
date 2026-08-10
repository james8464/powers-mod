package com.powers.item.artifact;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactScrollRulesTest {
	@Test
	void scrollInterceptionRequiresGameplayCrouchAndHeldArtifact() {
		assertTrue(ArtifactScrollRules.shouldIntercept(false, true, true, 1.0));
		assertFalse(ArtifactScrollRules.shouldIntercept(true, true, true, 1.0));
		assertFalse(ArtifactScrollRules.shouldIntercept(false, false, true, 1.0));
		assertFalse(ArtifactScrollRules.shouldIntercept(false, true, false, 1.0));
		assertFalse(ArtifactScrollRules.shouldIntercept(false, true, true, 0.0));
		assertFalse(ArtifactScrollRules.shouldIntercept(false, true, true, Double.NaN));
	}

	@Test
	void packetDirectionIsAlwaysBoundedToOneStep() {
		assertEquals(1, ArtifactScrollRules.direction(12.5));
		assertEquals(-1, ArtifactScrollRules.direction(-0.25));
		assertEquals(0, ArtifactScrollRules.direction(Double.POSITIVE_INFINITY));
		assertTrue(ArtifactScrollRules.validDirection(1));
		assertTrue(ArtifactScrollRules.validDirection(-1));
		assertFalse(ArtifactScrollRules.validDirection(2));
	}
}

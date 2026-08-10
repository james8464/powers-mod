package com.powers.power.artifact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArtifactCovenantRulesTest {
	@Test
	void sharesHalfOfRealDamageWithoutCreatingOrOverhealingDamage() {
		assertEquals(0.0F, ArtifactCovenantRules.sharedDamage(-1.0F));
		assertEquals(0.0F, ArtifactCovenantRules.sharedDamage(0.0F));
		assertEquals(3.75F, ArtifactCovenantRules.sharedDamage(7.5F));
	}

	@Test
	void ownerLinkCountIsStrictlyBounded() {
		assertTrue(ArtifactCovenantRules.mayAddLink(7, false));
		assertFalse(ArtifactCovenantRules.mayAddLink(8, false));
		assertTrue(ArtifactCovenantRules.mayAddLink(8, true));
	}

	@Test
	void expiryUsesOneMonotonicWorldClockAtItsExactBoundary() {
		assertFalse(ArtifactCovenantRules.expired(9_999_999L, 10_000_000L));
		assertTrue(ArtifactCovenantRules.expired(10_000_000L, 10_000_000L));
	}
}

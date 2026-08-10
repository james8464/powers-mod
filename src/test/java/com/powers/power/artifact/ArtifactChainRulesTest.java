package com.powers.power.artifact;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactChainRulesTest {
	@Test
	void chainRequiresLiveSameRealmVisibleUnsuppressedTargetsInsideRange() {
		assertTrue(ArtifactChainRules.active(100, 120, true, true,
				63.9 * 63.9, true, false, false, false));
		assertFalse(ArtifactChainRules.active(120, 120, true, true,
				1.0, true, false, false, false));
		assertFalse(ArtifactChainRules.active(100, 120, true, true,
				64.01 * 64.01, true, false, false, false));
		assertFalse(ArtifactChainRules.active(100, 120, true, true,
				1.0, false, false, false, false));
		assertFalse(ArtifactChainRules.active(100, 120, true, true,
				1.0, true, true, false, false));
	}
}

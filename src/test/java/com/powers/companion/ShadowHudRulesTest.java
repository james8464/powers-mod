package com.powers.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowHudRulesTest {
	@Test
	void hudIsOwnerOnlyAndOnlyVisibleForRelevantShadowState() {
		assertTrue(ShadowHudRules.visible(true, true, false));
		assertTrue(ShadowHudRules.visible(true, false, true));
		assertFalse(ShadowHudRules.visible(false, true, true));
		assertFalse(ShadowHudRules.visible(true, false, false));
	}
}

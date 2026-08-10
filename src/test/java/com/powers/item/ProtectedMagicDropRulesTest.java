package com.powers.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectedMagicDropRulesTest {
	@Test
	void crystalsAndMythicArtifactsShareOneDropProtectionPolicy() {
		assertTrue(ProtectedMagicDropRules.isProtectedCategory(true, false));
		assertTrue(ProtectedMagicDropRules.isProtectedCategory(false, true));
		assertTrue(ProtectedMagicDropRules.isProtectedCategory(true, true));
		assertFalse(ProtectedMagicDropRules.isProtectedCategory(false, false));
	}
}

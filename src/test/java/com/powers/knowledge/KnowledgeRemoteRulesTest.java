package com.powers.knowledge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeRemoteRulesTest {
	@Test
	void remoteFallbackRequiresLowConfidenceAndNeverGuessesRecipes() {
		assertTrue(KnowledgeRemoteRules.mayFallback("Who sealed the first vessel?", 0.2));
		assertFalse(KnowledgeRemoteRules.mayFallback("How do I craft powers:light_crystal?", 0.2));
		assertFalse(KnowledgeRemoteRules.mayFallback("What is stone?", 0.8));
		assertFalse(KnowledgeRemoteRules.mayFallback("", 0.0));
	}
}

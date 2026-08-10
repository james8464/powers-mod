package com.powers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportedItemRulesTest {
	@Test
	void textureCompositionLayersAreLegacyAliasesNotGameplayRunes() {
		assertTrue(ImportedItemRules.isLegacyAssetLayer("artifact_runestone_back"));
		assertTrue(ImportedItemRules.isLegacyAssetLayer("artifact_runestone_overlay_10"));
		assertFalse(ImportedItemRules.isLegacyAssetLayer("artifact_runestone_inert"));
		assertFalse(ImportedItemRules.isLegacyAssetLayer("artifact_runestone_dark_large"));
	}
}

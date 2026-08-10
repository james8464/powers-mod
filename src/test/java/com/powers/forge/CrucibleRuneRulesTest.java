package com.powers.forge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CrucibleRuneRulesTest {
	@Test
	void authoredRunesMapToFourExponentialTiers() {
		assertEquals(25, CrucibleRuneRules.xpFor("imported_artifact_runestone_inert"));
		assertEquals(75, CrucibleRuneRules.xpFor("imported_artifact_runestone_dark_tiny"));
		assertEquals(225, CrucibleRuneRules.xpFor("imported_artifact_runestone_dark_large"));
		assertEquals(675, CrucibleRuneRules.xpFor("imported_artifact_runestone_dark_inscribed_large"));
		assertEquals(0, CrucibleRuneRules.xpFor("diamond"));
	}

	@Test
	void archaeologicalAndEssenceRelicsAreUsefulBoundedInfusionCatalysts() {
		assertEquals(50, CrucibleRuneRules.xpFor("imported_magic_essence_soul_dust"));
		assertEquals(100, CrucibleRuneRules.xpFor("imported_magic_essence_sacred_dust"));
		assertEquals(125, CrucibleRuneRules.xpFor("imported_artifact_malignember"));
		assertEquals(175, CrucibleRuneRules.xpFor("imported_artifact_emperyeanjewel"));
		assertEquals(35, CrucibleRuneRules.xpFor("imported_artifact_trilobite_fossil"));
	}
}

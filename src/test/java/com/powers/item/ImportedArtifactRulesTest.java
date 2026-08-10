package com.powers.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImportedArtifactRulesTest {
	@Test
	void registeredRelicFamiliesHaveExplicitGameplayRoles() {
		assertEquals(ImportedArtifactKind.ATTUNEMENT,
				ImportedArtifactRules.kind("artifact_diamond_ring"));
		assertEquals(ImportedArtifactKind.SOUL_VESSEL,
				ImportedArtifactRules.kind("artifact_soulmatrix"));
		assertEquals(ImportedArtifactKind.RITUAL_CATALYST,
				ImportedArtifactRules.kind("artifact_ritualdagger"));
		assertEquals(ImportedArtifactKind.HEART_RELIC,
				ImportedArtifactRules.kind("artifact_ghoul_heart"));
		assertEquals(ImportedArtifactKind.TRANSMUTER,
				ImportedArtifactRules.kind("artifact_philosopherstone"));
		assertEquals(ImportedArtifactKind.TRAVEL_RELIC,
				ImportedArtifactRules.kind("device_miniportal"));
		assertEquals(ImportedArtifactKind.COMMAND_RELIC,
				ImportedArtifactRules.kind("artifact_flute"));
		assertEquals(ImportedArtifactKind.ARCANE_CATALYST,
				ImportedArtifactRules.kind("magic_essence_soul_dust"));
		assertEquals(ImportedArtifactKind.LORE_RELIC,
				ImportedArtifactRules.kind("artifact_trilobite_fossil"));
	}

	@Test
	void ordinaryFoodsAndTextureLayersAreNotPromotedToRelics() {
		assertEquals(ImportedArtifactKind.NONE,
				ImportedArtifactRules.kind("food_fig"));
		assertEquals(ImportedArtifactKind.NONE,
				ImportedArtifactRules.kind("artifact_runestone_overlay_4"));
	}

	@Test
	void passiveAndSoulBudgetsAreFiniteAcrossStackCountsAndTiers() {
		assertEquals(0, ImportedArtifactRules.attunementEnergy(-4));
		assertEquals(6, ImportedArtifactRules.attunementEnergy(99));
		assertEquals(14, ImportedArtifactRules.soulDrain("artifact_soulstone_small"));
		assertEquals(60, ImportedArtifactRules.soulDrain("artifact_soulmatrix"));
	}
}

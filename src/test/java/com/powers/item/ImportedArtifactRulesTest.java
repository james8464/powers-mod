package com.powers.item;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportedArtifactRulesTest {
	@Test
	void registeredRelicFamiliesHaveExplicitGameplayRoles() {
		assertEquals(ImportedArtifactKind.ATTUNEMENT,
				ImportedArtifactRules.kind("artifact_diamond_ring"));
		assertEquals(ImportedArtifactKind.ENERGY_RESERVOIR,
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
	void passiveBudgetsAreFiniteAcrossStackCounts() {
		assertEquals(0, ImportedArtifactRules.attunementEnergy(-4));
		assertEquals(6, ImportedArtifactRules.attunementEnergy(99));
	}

	@Test
	void attunementsAndHeartsHaveDistinctBoundedIdentities() {
		assertEquals(1, ImportedArtifactRules.attunementEnergy("artifact_corroded_copper_ring"));
		assertEquals(2, ImportedArtifactRules.attunementEnergy("artifact_emerald_ring"));
		assertEquals(3, ImportedArtifactRules.attunementEnergy("artifact_diamond_ring"));
		assertEquals(ImportedArtifactRules.HeartSpecialization.BLOOD_WARD,
				ImportedArtifactRules.heartSpecialization("artifact_bloodstone"));
		assertEquals(ImportedArtifactRules.HeartSpecialization.CLOCKWORK,
				ImportedArtifactRules.heartSpecialization("artifact_heart_mechanism"));
		assertEquals(ImportedArtifactRules.HeartSpecialization.WILDWOOD,
				ImportedArtifactRules.heartSpecialization("artifact_woodheart"));
	}

	@Test
	void everyRingAndAmuletHasOneNamedNumericalAttunement() {
		assertProfile("artifact_diamond_ring", ImportedArtifactRules.AttunementSchool.WARDING,
				ImportedArtifactRules.AttunementEffect.WOUNDED_RESISTANCE, 2);
		assertProfile("artifact_emerald_ring", ImportedArtifactRules.AttunementSchool.VERDANCY,
				ImportedArtifactRules.AttunementEffect.WOUNDED_REGENERATION, 1);
		assertProfile("artifact_amulet", ImportedArtifactRules.AttunementSchool.CHANNELING,
				ImportedArtifactRules.AttunementEffect.CHANNEL_RECOVERY, 2);
		assertProfile("artifact_plain_copper_ring", ImportedArtifactRules.AttunementSchool.RECOVERY,
				ImportedArtifactRules.AttunementEffect.LOW_ENERGY_RECOVERY, 2);
		assertProfile("artifact_corroded_copper_ring", ImportedArtifactRules.AttunementSchool.RECOVERY,
				ImportedArtifactRules.AttunementEffect.LOW_ENERGY_RECOVERY, 1);
	}

	@Test
	void strongestProfilePerSchoolWinsWithoutMultiplicativeStacking() {
		Map<ImportedArtifactRules.AttunementSchool, ImportedArtifactRules.AttunementProfile> strongest =
				ImportedArtifactRules.strongestAttunements(List.of(
						"artifact_corroded_copper_ring", "artifact_plain_copper_ring",
						"artifact_plain_copper_ring", "artifact_amulet", "artifact_amulet"));
		assertEquals(2, strongest.size());
		assertEquals("artifact_plain_copper_ring",
				strongest.get(ImportedArtifactRules.AttunementSchool.RECOVERY).texture());
		assertEquals(2,
				strongest.get(ImportedArtifactRules.AttunementSchool.CHANNELING).magnitude());
	}

	@Test
	void situationalEnergyRecoveryRemainsExplicitAndGloballyBounded() {
		Map<ImportedArtifactRules.AttunementSchool, ImportedArtifactRules.AttunementProfile> strongest =
				ImportedArtifactRules.strongestAttunements(List.of(
						"artifact_plain_copper_ring", "artifact_corroded_copper_ring",
						"artifact_amulet", "artifact_diamond_ring"));
		assertEquals(0, ImportedArtifactRules.attunementEnergy(strongest, false, false));
		assertEquals(2, ImportedArtifactRules.attunementEnergy(strongest, true, false));
		assertEquals(2, ImportedArtifactRules.attunementEnergy(strongest, false, true));
		assertEquals(4, ImportedArtifactRules.attunementEnergy(strongest, true, true));
		assertEquals(6, ImportedArtifactRules.cappedPassiveEnergy(99, true, true));
	}

	@Test
	void situationalThresholdsAreInclusiveAndRejectInvalidCapacities() {
		assertEquals(true, ImportedArtifactRules.wounded(10.0F, 20.0F));
		assertEquals(false, ImportedArtifactRules.wounded(10.01F, 20.0F));
		assertEquals(false, ImportedArtifactRules.wounded(0.0F, 0.0F));
		assertEquals(true, ImportedArtifactRules.lowEnergy(250, 1_000));
		assertEquals(false, ImportedArtifactRules.lowEnergy(251, 1_000));
		assertEquals(false, ImportedArtifactRules.lowEnergy(0, 0));
	}

	private static void assertProfile(String texture, ImportedArtifactRules.AttunementSchool school,
			ImportedArtifactRules.AttunementEffect effect, int magnitude) {
		ImportedArtifactRules.AttunementProfile profile = ImportedArtifactRules.attunementProfile(texture);
		assertEquals(texture, profile.texture());
		assertEquals(school, profile.school());
		assertEquals(effect, profile.effect());
		assertEquals(magnitude, profile.magnitude());
	}

	@Test
	void heartEatingHonorsExplicitAlignmentWithoutLockingClockwork() {
		assertTrue(ImportedArtifactRules.mayEatHeart(
				ImportedArtifactRules.HeartSpecialization.LIVING, false));
		assertFalse(ImportedArtifactRules.mayEatHeart(
				ImportedArtifactRules.HeartSpecialization.LIVING, true));
		assertTrue(ImportedArtifactRules.mayEatHeart(
				ImportedArtifactRules.HeartSpecialization.GHOUL, true));
		assertFalse(ImportedArtifactRules.mayEatHeart(
				ImportedArtifactRules.HeartSpecialization.BLOOD_WARD, false));
		assertTrue(ImportedArtifactRules.mayEatHeart(
				ImportedArtifactRules.HeartSpecialization.CLOCKWORK, false));
		assertTrue(ImportedArtifactRules.mayEatHeart(
				ImportedArtifactRules.HeartSpecialization.CLOCKWORK, true));
	}

	@Test
	void onlyOneDeterministicStrongestHeartCanOwnThePassive() {
		assertEquals(ImportedArtifactRules.HeartSpecialization.BLOOD_WARD,
				ImportedArtifactRules.strongestHeart(List.of(
						ImportedArtifactRules.HeartSpecialization.LIVING,
						ImportedArtifactRules.HeartSpecialization.CLOCKWORK,
						ImportedArtifactRules.HeartSpecialization.BLOOD_WARD,
						ImportedArtifactRules.HeartSpecialization.GHOUL)));
		assertEquals(ImportedArtifactRules.HeartSpecialization.WILDWOOD,
				ImportedArtifactRules.strongestHeart(List.of(
						ImportedArtifactRules.HeartSpecialization.WILDWOOD,
						ImportedArtifactRules.HeartSpecialization.LIVING)));
		assertEquals(ImportedArtifactRules.HeartSpecialization.NONE,
				ImportedArtifactRules.strongestHeart(List.of()));
		assertEquals(1, ImportedArtifactRules.heartTier(
				ImportedArtifactRules.HeartSpecialization.LIVING));
		assertEquals(5, ImportedArtifactRules.heartTier(
				ImportedArtifactRules.HeartSpecialization.BLOOD_WARD));
	}
}

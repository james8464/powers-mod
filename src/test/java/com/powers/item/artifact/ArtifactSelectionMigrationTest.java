package com.powers.item.artifact;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArtifactSelectionMigrationTest {
	@Test
	void currentAndRoutedKeysAreStable() {
		assertEquals("unique/blight_ground", ArtifactSelectionMigration.migrate(
				ArtifactAlignment.DARKNESS, "unique/blight_ground", 4));
		assertEquals("innate/fireball", ArtifactSelectionMigration.migrate(
				ArtifactAlignment.DARKNESS, "innate/fireball", 10));
		assertEquals("crystal/inferno", ArtifactSelectionMigration.migrate(
				ArtifactAlignment.DARKNESS, "crystal/inferno", 10));
		assertEquals("dominion/call_radiant", ArtifactSelectionMigration.migrate(
				ArtifactAlignment.LIGHT, "dominion/call_radiant", 10));
	}

	@Test
	void legacyUtilitySelectionsMapToTheirCanonicalSuccessors() {
		assertEquals("unique/call_hollowed", ArtifactSelectionMigration.migrate(
				ArtifactAlignment.DARKNESS, "legacy/summon_darkness", 1));
		assertEquals("unique/blight_ground", ArtifactSelectionMigration.migrate(
				ArtifactAlignment.DARKNESS, "dominion/blight_ground", 2));
	}

	@Test
	void retiredDestructiveSelectionsRequireRankTenForNightfall() {
		for (String legacy : java.util.List.of(
				"legacy/abyssal_singularity", "legacy/oblivion_pulse",
				"legacy/annihilation_beam", "legacy/soul_requiem",
				"dominion/event_horizon", "dominion/legion_eclipse")) {
			assertEquals("unique/call_hollowed", ArtifactSelectionMigration.migrate(
					ArtifactAlignment.DARKNESS, legacy, 9), legacy);
			assertEquals("unique/nightfall_dominion", ArtifactSelectionMigration.migrate(
					ArtifactAlignment.DARKNESS, legacy, 10), legacy);
		}
	}

	@Test
	void legacyNightfallCannotBypassItsRankGate() {
		assertEquals("unique/call_hollowed", ArtifactSelectionMigration.migrate(
				ArtifactAlignment.DARKNESS, "legacy/nightfall_dominion", 9));
		assertEquals("unique/nightfall_dominion", ArtifactSelectionMigration.migrate(
				ArtifactAlignment.DARKNESS, "legacy/nightfall_dominion", 10));
	}

	@Test
	void canonicalNightfallAlsoRemainsRankGatedDuringLoad() {
		assertEquals("unique/call_hollowed", ArtifactSelectionMigration.migrate(
				ArtifactAlignment.DARKNESS, "unique/nightfall_dominion", 9));
		assertEquals("unique/nightfall_dominion", ArtifactSelectionMigration.migrate(
				ArtifactAlignment.DARKNESS, "unique/nightfall_dominion", 10));
	}

	@Test
	void unknownSelectionsUseTheStableRoutedFallback() {
		assertEquals("innate/lightning_strike", ArtifactSelectionMigration.migrate(
				ArtifactAlignment.DARKNESS, "removed/by_another_mod", 10));
	}

	@Test
	void retiredPartisanRitesMigrateToTheirNearestCuratedSuccessors() {
		assertEquals("innate/flight", ArtifactSelectionMigration.migrate(
				ArtifactAlignment.LIGHT, "dominion/dawnstride", 10));
		assertEquals("dominion/consecrate_ground", ArtifactSelectionMigration.migrate(
				ArtifactAlignment.LIGHT, "dominion/banish_darkness", 10));
		assertEquals("dominion/daybreak_wave", ArtifactSelectionMigration.migrate(
				ArtifactAlignment.LIGHT, "dominion/divine_decree", 10));
	}
}

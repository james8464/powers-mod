package com.powers.item.artifact;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArtifactFavouriteRulesTest {
	@Test
	void darknessDefaultsUseTheLockedCombatOrder() {
		List<String> favourites = ArtifactFavouriteRules.defaults(
				ArtifactActionCatalogue.forAlignment(ArtifactAlignment.DARKNESS), 1,
				"innate/time_shift");
		assertEquals(List.of(
				"innate/lightning_strike",
				"innate/fireball",
				"innate/shadow_step",
				"innate/forcefield",
				"innate/flight",
				"unique/call_hollowed",
				"unique/blight_ground",
				"unique/nightfall_dominion"), favourites);
	}

	@Test
	void assigningAnExistingFavouriteSwapsSlotsWithoutDuplicates() {
		List<String> assigned = ArtifactFavouriteRules.assign(
				List.of("a", "b", "c", "d", "e", "f", "g", "h"), 0, "c");
		assertEquals(List.of("c", "b", "a", "d", "e", "f", "g", "h"), assigned);
		assertEquals(assigned, ArtifactFavouriteRules.assign(assigned, -1, "x"));
	}

	@Test
	void reconcileMigratesRetiredKeysDeduplicatesAndFillsDefaults() {
		List<String> reconciled = ArtifactFavouriteRules.reconcile(
				List.of("unique/summon_darkness", "unique/call_hollowed",
						"unique/spread_darkness", "unknown"),
				ArtifactActionCatalogue.forAlignment(ArtifactAlignment.DARKNESS),
				ArtifactAlignment.DARKNESS, 4, "innate/lightning_strike");

		assertEquals(8, reconciled.size());
		assertEquals("unique/call_hollowed", reconciled.get(0));
		assertEquals("unique/blight_ground", reconciled.get(1));
		assertEquals(8, reconciled.stream().filter(key -> !key.isBlank()).distinct().count());
	}

	@Test
	void cyclingUsesOnlyNonBlankFavouriteSlots() {
		List<String> favourites = List.of("lightning", "", "fireball", "", "", "", "", "");
		assertEquals("fireball", ArtifactFavouriteRules.cycle(favourites, "lightning", 1));
		assertEquals("lightning", ArtifactFavouriteRules.cycle(favourites, "fireball", 1));
		assertEquals("fireball", ArtifactFavouriteRules.cycle(favourites, "lightning", -1));
	}
}

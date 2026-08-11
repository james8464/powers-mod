package com.powers.item.artifact;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactCatalogueRulesTest {
	@Test
	void searchMatchesLocalizedLabelsStableIdsAndSelectedTab() {
		List<ArtifactActionDefinition> actions = ArtifactActionCatalogue.forAlignment(
				ArtifactAlignment.DARKNESS);
		var lightning = ArtifactCatalogueRules.filter(actions,
				ArtifactActionCategory.ROUTED_POWER, "storm bolt",
				action -> action.abilityId().equals("lightning_strike") ? "Storm Bolt" : "Other");
		assertEquals(List.of("lightning_strike"), lightning.stream()
				.map(ArtifactActionDefinition::abilityId).toList());

		var dominion = ArtifactCatalogueRules.filter(actions,
				ArtifactActionCategory.DOMINION, "nightfall-dominion",
				action -> action.abilityId());
		assertEquals(List.of("nightfall_dominion"), dominion.stream()
				.map(ArtifactActionDefinition::abilityId).toList());
	}

	@Test
	void combatTabsSeparateFavouritesInnateCrystalsAndSwordActions() {
		List<ArtifactActionDefinition> actions = ArtifactActionCatalogue.forAlignment(
				ArtifactAlignment.DARKNESS);
		List<String> favourites = List.of("unique/blight_ground", "innate/lightning_strike");

		assertEquals(List.of("unique/blight_ground", "innate/lightning_strike"),
				ArtifactCatalogueRules.filter(actions, ArtifactCatalogueTab.FAVOURITES,
						favourites, "", ArtifactActionDefinition::abilityId).stream()
						.map(ArtifactActionDefinition::key).toList());
		assertTrue(ArtifactCatalogueRules.filter(actions, ArtifactCatalogueTab.INNATE,
				favourites, "", ArtifactActionDefinition::abilityId).stream()
				.allMatch(action -> action.category() == ArtifactActionCategory.ROUTED_POWER));
		assertTrue(ArtifactCatalogueRules.filter(actions, ArtifactCatalogueTab.CRYSTALS,
				favourites, "", ArtifactActionDefinition::abilityId).stream()
				.allMatch(action -> action.category() == ArtifactActionCategory.ROUTED_CRYSTAL));
		assertTrue(ArtifactCatalogueRules.filter(actions, ArtifactCatalogueTab.SWORD,
				favourites, "", ArtifactActionDefinition::abilityId).stream()
				.allMatch(action -> action.category() == ArtifactActionCategory.DOMINION));
	}

	@Test
	void layoutFitsSmallScreensAndExpandsToTwoColumns() {
		ArtifactCatalogueRules.Layout compact = ArtifactCatalogueRules.layout(320, 240);
		assertTrue(compact.panelX() >= 8 && compact.panelY() >= 8);
		assertTrue(compact.panelX() + compact.panelWidth() <= 312);
		assertTrue(compact.panelY() + compact.panelHeight() <= 232);
		assertEquals(1, compact.columns());
		assertTrue(compact.pageSize() >= 4);

		ArtifactCatalogueRules.Layout wide = ArtifactCatalogueRules.layout(1280, 720);
		assertEquals(2, wide.columns());
		assertTrue(wide.pageSize() > compact.pageSize());

		ArtifactCatalogueRules.Layout narrow = ArtifactCatalogueRules.layout(240, 180);
		assertTrue(narrow.panelX() >= 4 && narrow.panelY() >= 4);
		assertTrue(narrow.panelX() + narrow.panelWidth() <= 236);
		assertTrue(narrow.panelY() + narrow.panelHeight() <= 176);
	}
}

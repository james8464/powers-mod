package com.powers.item.artifact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powers.power.crystals.CrystalAbilityCatalog;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArtifactActionCatalogueTest {
	@Test
	void shadowRoutesEverythingWhilePartisanUsesAStrictRadiantSubset() {
		Set<String> crystalIds = new HashSet<>();
		CrystalAbilityCatalog.defaults().values().forEach(crystalIds::addAll);
		var shadow = ArtifactActionCatalogue.forAlignment(ArtifactAlignment.DARKNESS);
		var partisan = ArtifactActionCatalogue.forAlignment(ArtifactAlignment.LIGHT);
		for (var actions : java.util.List.of(shadow, partisan)) assertEquals(actions.size(),
				actions.stream().map(ArtifactActionDefinition::key).distinct().count());
		assertEquals(23, shadow.stream().filter(action -> action.category()
				== ArtifactActionCategory.ROUTED_POWER).count());
		assertEquals(crystalIds, shadow.stream().filter(action -> action.category()
				== ArtifactActionCategory.ROUTED_CRYSTAL).map(ArtifactActionDefinition::abilityId)
				.collect(java.util.stream.Collectors.toSet()));
		assertEquals(Set.of("flight", "starfall", "lightning_strike", "thunderclap",
				"energy_beam", "forcefield", "plant_healing_acceleration", "double_health"),
				partisan.stream().filter(action -> action.category() == ArtifactActionCategory.ROUTED_POWER)
						.map(ArtifactActionDefinition::abilityId).collect(java.util.stream.Collectors.toSet()));
		assertEquals(Set.of("creativity_manifestation", "life_bloom", "light_crystal"),
				partisan.stream().filter(action -> action.category() == ArtifactActionCategory.ROUTED_CRYSTAL)
						.map(ArtifactActionDefinition::abilityId).collect(java.util.stream.Collectors.toSet()));
		assertTrue(partisan.size() < shadow.size());
	}

	@Test
	void darknessExposesOnlyTheThreeCanonicalOriginals() {
		Set<String> keys = ArtifactActionCatalogue.forAlignment(ArtifactAlignment.DARKNESS).stream()
				.filter(action -> action.category() == ArtifactActionCategory.DOMINION)
				.map(ArtifactActionDefinition::key).collect(java.util.stream.Collectors.toSet());

		assertEquals(Set.of("unique/call_hollowed", "unique/blight_ground",
				"unique/nightfall_dominion"), keys);
	}

	@Test
	void lightKeepsEightCuratedPartisanRites() {
		Set<String> ids = ArtifactActionCatalogue.forAlignment(ArtifactAlignment.LIGHT).stream()
				.filter(action -> action.category() == ArtifactActionCategory.DOMINION)
				.map(ArtifactActionDefinition::abilityId).collect(java.util.stream.Collectors.toSet());
		assertEquals(Set.of("call_radiant", "consecrate_ground", "covenant_chain",
				"daybreak_wave", "heaven_gate",
				"solar_firmament", "second_dawn", "host_heaven"), ids);
	}

	@Test
	void everyActionHasVisibleCostGateAndCooldownMetadata() {
		ArtifactActionCatalogue.all().forEach(action -> {
			assertTrue(action.energyCost() > 0, action.key());
			assertTrue(action.requiredRank() >= 1 && action.requiredRank() <= 10, action.key());
			assertTrue(action.baseCooldownTicks() >= 0, action.key());
		});
	}
}

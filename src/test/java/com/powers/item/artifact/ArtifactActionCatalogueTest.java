package com.powers.item.artifact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powers.power.crystals.CrystalAbilityCatalog;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArtifactActionCatalogueTest {
	@Test
	void bothArtifactsRouteTheCompletePowerRosterWithoutDuplicateKeys() {
		Set<String> crystalIds = new HashSet<>();
		CrystalAbilityCatalog.defaults().values().forEach(crystalIds::addAll);
		for (ArtifactAlignment alignment : ArtifactAlignment.values()) {
			var actions = ArtifactActionCatalogue.forAlignment(alignment);
			assertEquals(actions.size(), actions.stream().map(ArtifactActionDefinition::key).distinct().count());
			assertEquals(28, actions.stream().filter(action -> action.category()
					== ArtifactActionCategory.ROUTED_POWER).count());
			assertEquals(crystalIds, actions.stream().filter(action -> action.category()
					== ArtifactActionCategory.ROUTED_CRYSTAL).map(ArtifactActionDefinition::abilityId)
					.collect(java.util.stream.Collectors.toSet()));
		}
	}

	@Test
	void darknessPreservesLegacyInvocationsAndAddsElevenAuthoredDominionPowers() {
		Set<String> ids = ArtifactActionCatalogue.forAlignment(ArtifactAlignment.DARKNESS).stream()
				.filter(action -> action.category() == ArtifactActionCategory.DOMINION)
				.map(ArtifactActionDefinition::abilityId).collect(java.util.stream.Collectors.toSet());
		assertTrue(ids.containsAll(Set.of("abyssal_singularity", "oblivion_pulse",
				"annihilation_beam", "soul_requiem", "nightfall_dominion")));
		assertTrue(ids.containsAll(Set.of("call_hollowed", "blight_ground", "umbral_step",
				"night_chain", "eclipse_wave", "abyss_gate", "devour_light", "black_decree",
				"event_horizon", "deathless_night", "legion_eclipse")));
	}

	@Test
	void lightHasAllElevenAuthoredPartisanPowers() {
		Set<String> ids = ArtifactActionCatalogue.forAlignment(ArtifactAlignment.LIGHT).stream()
				.filter(action -> action.category() == ArtifactActionCategory.DOMINION)
				.map(ArtifactActionDefinition::abilityId).collect(java.util.stream.Collectors.toSet());
		assertEquals(Set.of("call_radiant", "consecrate_ground", "dawnstride", "covenant_chain",
				"daybreak_wave", "heaven_gate", "banish_darkness", "divine_decree",
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

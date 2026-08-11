package com.powers.magic;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicActionCatalogueTest {
	private static final Map<MagicOrigin, Integer> EXPECTED_ORIGIN_COUNTS = Map.of(
			MagicOrigin.INNATE, 23,
			MagicOrigin.CRYSTAL, 11,
			MagicOrigin.ARTIFACT, 13,
			MagicOrigin.SPELL, 12,
			MagicOrigin.AMETHYST, 3,
			MagicOrigin.REALM, 2);

	@Test
	void defaultsContainEverySupportedActionExactlyOnce() {
		MagicActionCatalogue catalogue = MagicActionCatalogue.defaults();
		Map<MagicOrigin, Integer> actualCounts = new EnumMap<>(MagicOrigin.class);
		catalogue.definitions().forEach(definition ->
				actualCounts.merge(definition.origin(), 1, Integer::sum));

		assertEquals(64, catalogue.definitions().size());
		assertEquals(64, catalogue.definitions().stream()
				.map(MagicActionDefinition::id).distinct().count());
		assertEquals(EXPECTED_ORIGIN_COUNTS, actualCounts);
		assertTrue(catalogue.definitions().stream().allMatch(MagicActionDefinition::isComplete));
	}

	@Test
	void catalogueUsesStableIdsForEveryExistingRegistry() {
		MagicActionCatalogue catalogue = MagicActionCatalogue.defaults();
		Set<String> actual = catalogue.definitions().stream()
				.map(definition -> definition.id().value())
				.collect(Collectors.toUnmodifiableSet());

		assertTrue(actual.containsAll(Set.of(
				"size_morph", "time_shift", "flight", "starfall", "void_beam", "fireball", "lightning_strike",
				"thunderclap", "speed_burst", "telekinesis", "energy_beam", "super_speed",
				"breezy_bash", "invisibility", "time_freeze", "forcefield",
				"gravity_displacement", "vessel_possession", "astral_projection", "energy_drain",
				"ice_manipulation", "plant_healing_acceleration", "double_health",
				"inferno", "clone_swarm", "creativity_manifestation", "size_shift", "life_bloom",
				"chrono_stop", "dreamwalking", "middleworld",
				"soul_link", "light_crystal", "dark_crystal",
				"call_hollowed", "blight_ground", "nightfall_dominion",
				"soul_compass", "augury", "cartographers_star", "celestial_ruin", "dimensional_anchor",
				"blood_reading", "grave_recall", "purification_circle", "verdant_tending",
				"hearth_sanctuary", "ward_breaking_ritual", "dispel",
				"amethyst_item", "amethyst_block", "amethyst_ward",
				"darkness_block", "pure_light_block")));
		assertTrue(catalogue.definition(new MagicActionId("slow_world")) == null);
		for (String retired : Set.of("cozy_campfire", "frost_nova", "elemental_blast",
				"ground_slam", "shadow_step")) {
			assertTrue(catalogue.definition(new MagicActionId(retired)) == null, retired);
		}
		assertTrue(catalogue.definition(new MagicActionId("annihilation_beam")) == null);
		assertTrue(catalogue.definition(new MagicActionId("legion_eclipse")) == null);
		assertTrue(catalogue.definition(new MagicActionId("portal_rift")) == null);
		assertEquals(MagicOrigin.INNATE,
				catalogue.definition(new MagicActionId("size_morph")).origin());
		assertEquals(MagicOrigin.CRYSTAL,
				catalogue.definition(new MagicActionId("size_shift")).origin());
	}

	@Test
	void definitionsRejectIncompleteOrUnsafeValues() {
		MagicActionDefinition valid = MagicActionCatalogue.defaults().definition(new MagicActionId("fireball"));
		assertTrue(valid.baseRange() > 0.0);
		assertTrue(valid.baseEnergy() > 0);
		assertTrue(valid.baseCooldownTicks() > 0);
		assertTrue(valid.residueTicks() > 0);
	}

	@Test
	void everyActionDeclaresItsPresentationSignificanceExplicitly() {
		MagicActionCatalogue catalogue = MagicActionCatalogue.defaults();

		assertTrue(catalogue.definitions().stream()
				.allMatch(definition -> definition.significance() != null));
		assertEquals(MagicSignificance.MINIMAL,
				catalogue.definition(new MagicActionId("lightning_strike")).significance());
		assertEquals(MagicSignificance.MINIMAL,
				catalogue.definition(new MagicActionId("fireball")).significance());
		assertEquals(MagicSignificance.RITUAL,
				catalogue.definition(new MagicActionId("dimensional_anchor")).significance());
		assertEquals(MagicSignificance.COSMIC,
				catalogue.definition(new MagicActionId("time_freeze")).significance());
		assertEquals(MagicSignificance.COSMIC,
				catalogue.definition(new MagicActionId("celestial_ruin")).significance());
		assertEquals(MagicSignificance.NONE,
				catalogue.definition(new MagicActionId("darkness_block")).significance());
		assertEquals(MagicSignificance.NONE,
				catalogue.definition(new MagicActionId("amethyst_item")).significance());
	}

	@Test
	void everyRegisteredActionDeclaresAnEntityTargetContract() {
		MagicActionCatalogue catalogue = MagicActionCatalogue.defaults();
		assertEquals(64, catalogue.definitions().stream()
				.filter(definition -> definition.targetContract() != null).count());
		assertEquals(ActionTargetContract.PLAYER_OR_MOB_FALLBACK,
				catalogue.definition(new MagicActionId("energy_drain")).targetContract());
		assertEquals(ActionTargetContract.PLAYER_PARTICIPANT,
				catalogue.definition(new MagicActionId("light_crystal")).targetContract());
		assertEquals(ActionTargetContract.NONE,
				catalogue.definition(new MagicActionId("flight")).targetContract());
	}
}

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
			MagicOrigin.INNATE, 27,
			MagicOrigin.CRYSTAL, 13,
			MagicOrigin.ARTIFACT, 7,
			MagicOrigin.SPELL, 21,
			MagicOrigin.AMETHYST, 3,
			MagicOrigin.REALM, 2);

	@Test
	void defaultsContainEverySupportedActionExactlyOnce() {
		MagicActionCatalogue catalogue = MagicActionCatalogue.defaults();
		Map<MagicOrigin, Integer> actualCounts = new EnumMap<>(MagicOrigin.class);
		catalogue.definitions().forEach(definition ->
				actualCounts.merge(definition.origin(), 1, Integer::sum));

		assertEquals(73, catalogue.definitions().size());
		assertEquals(73, catalogue.definitions().stream()
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
				"time_shift", "shadow_step", "flight", "elemental_blast",
				"starfall", "void_beam", "fireball", "frost_nova", "lightning_strike",
				"ground_slam", "thunderclap", "speed_burst", "telekinesis", "energy_beam", "super_speed",
				"breezy_bash", "cozy_campfire", "invisibility", "time_freeze", "forcefield",
				"gravity_displacement", "vessel_possession", "astral_projection", "energy_drain",
				"ice_manipulation", "plant_healing_acceleration", "double_health",
				"inferno", "clone_swarm", "creativity_manifestation", "size_shift", "life_bloom",
				"space_time", "chrono_stop", "dreamwalking", "portal_rift", "middleworld",
				"soul_link", "light_crystal", "dark_crystal",
				"summon_darkness", "spread_darkness", "abyssal_singularity",
				"oblivion_pulse", "annihilation_beam", "soul_requiem", "nightfall_dominion",
				"soul_compass", "tracking_mark", "weather_sigil", "celestial_ruin", "dimensional_anchor",
				"binding_sigil", "anti_portal_field", "kinetic_ward", "vitality_transfer",
				"hex", "concealment_veil", "purification_circle", "root_binding",
				"sanctuary_growth", "infernal_seal", "banishment_circle", "controlled_hellfire",
				"ward_breaking_ritual", "counterspell", "dispel", "ritual_amplification",
				"amethyst_item", "amethyst_block", "amethyst_ward",
				"darkness_block", "pure_light_block")));
		assertTrue(catalogue.definition(new MagicActionId("slow_world")) == null);
	}

	@Test
	void definitionsRejectIncompleteOrUnsafeValues() {
		MagicActionDefinition valid = MagicActionCatalogue.defaults().definition(new MagicActionId("fireball"));
		assertTrue(valid.baseRange() > 0.0);
		assertTrue(valid.baseEnergy() > 0);
		assertTrue(valid.baseCooldownTicks() > 0);
		assertTrue(valid.residueTicks() > 0);
	}
}

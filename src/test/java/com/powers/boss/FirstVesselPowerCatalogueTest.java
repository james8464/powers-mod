package com.powers.boss;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirstVesselPowerCatalogueTest {
	private static final Set<String> INNATE = Set.of(
			"size_shift", "time_shift", "shadow_step", "flight", "elemental_blast",
			"starfall", "void_beam", "fireball", "frost_nova", "lightning_strike",
			"ground_slam", "thunderclap", "speed_burst", "telekinesis", "energy_beam",
			"super_speed", "breezy_bash", "cozy_campfire", "invisibility", "time_freeze",
			"forcefield", "gravity_displacement", "vessel_possession", "astral_projection",
			"energy_drain", "ice_manipulation", "plant_healing_acceleration", "double_health");

	@Test
	void everyInnatePowerHasExactlyOneEntitySafeAdapter() {
		var actions = FirstVesselPowerCatalogue.actions();
		assertEquals(INNATE, actions.stream().map(FirstVesselPowerAction::powerId)
				.collect(java.util.stream.Collectors.toSet()));
		assertEquals(INNATE.size(), actions.size());
		assertTrue(actions.stream().allMatch(action -> action.cooldownTicks() >= 10
				&& action.weight() > 0));
	}

	@Test
	void phaseDecksNeverEscapeTheTwentyFourCandidateBudget() {
		for (FirstVesselPhase phase : FirstVesselPhase.values()) {
			assertTrue(FirstVesselPowerCatalogue.deck(phase).size()
					<= FirstVesselRules.MAX_CANDIDATES);
		}
	}
}

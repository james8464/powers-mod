package com.powers.power.crystals;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrystalAbilityCatalogTest {
	@Test
	void everyRegisteredCrystalHasAReachableAbilityAndEveryImplementedAbilityIsUsed() {
		var bindings = CrystalAbilityCatalog.defaults();
		assertEquals(Set.of("red_crystal", "orange_crystal", "yellow_crystal", "green_crystal",
				"blue_crystal", "indigo_crystal", "violet_crystal", "rainbow_crystal",
				"infected_rainbow_crystal", "light_crystal", "dark_crystal"), bindings.keySet());
		bindings.values().forEach(list -> assertFalse(list.isEmpty()));
		Set<String> reachable = new HashSet<>();
		bindings.values().forEach(reachable::addAll);
		assertEquals(Set.of("inferno", "creativity_manifestation", "clone_swarm", "size_shift",
				"space_time", "life_bloom", "dreamwalking", "middleworld",
				"soul_link", "chrono_stop", "light_crystal", "dark_crystal"), reachable);
		assertEquals(java.util.List.of("middleworld"), bindings.get("indigo_crystal"));
	}

	@Test
	void modeStateStartsAtFirstChoiceWrapsAndCanBeForgotten() {
		CrystalModeState state = new CrystalModeState();
		var player = java.util.UUID.randomUUID();
		assertEquals(0, state.current(player, 3));
		assertEquals(1, state.advance(player, 3));
		assertEquals(2, state.advance(player, 3));
		assertEquals(0, state.advance(player, 3));
		state.clear(player);
		assertEquals(0, state.current(player, 3));
	}

	@Test
	void crystalRadiusChecksAreSphericalAndInclusive() {
		assertTrue(CrystalTargeting.withinRadius(400.0, 20.0));
		assertFalse(CrystalTargeting.withinRadius(400.01, 20.0));
	}

	@Test
	void mindscapeCrystalsAlwaysHaveAServerChosenTraveller() {
		assertEquals(CrystalTargeting.JourneyTarget.CASTER,
				CrystalTargeting.journeyTarget(false, false));
		assertEquals(CrystalTargeting.JourneyTarget.CASTER,
				CrystalTargeting.journeyTarget(true, true));
		assertEquals(CrystalTargeting.JourneyTarget.AIMED_PLAYER,
				CrystalTargeting.journeyTarget(false, true));
	}
}

package com.powers.power.crystals;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
				"space_time", "life_bloom", "dreamwalking", "middleworld", "portal_rift",
				"soul_link", "chrono_stop", "light_crystal", "dark_crystal"), reachable);
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
}

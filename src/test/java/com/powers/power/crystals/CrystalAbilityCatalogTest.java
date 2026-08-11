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
				"light_crystal", "dark_crystal"), bindings.keySet());
		bindings.values().forEach(list -> assertFalse(list.isEmpty()));
		Set<String> reachable = new HashSet<>();
		bindings.values().forEach(reachable::addAll);
		assertEquals(Set.of("inferno", "creativity_manifestation", "clone_swarm", "size_shift",
				"life_bloom", "dreamwalking", "middleworld",
				"soul_link", "chrono_stop", "light_crystal", "dark_crystal"), reachable);
		assertEquals(java.util.List.of("life_bloom"), bindings.get("green_crystal"));
		assertEquals(java.util.List.of("middleworld"), bindings.get("indigo_crystal"));
		assertEquals(7, bindings.get("rainbow_crystal").size(),
				"Sevenfold Convergence must expose one mode for every coloured force");
		assertTrue(bindings.get("rainbow_crystal").contains("middleworld"),
				"The Indigo force must not disappear from the Rainbow convergence");
		assertEquals("soul_link", bindings.get("rainbow_crystal").get(5),
				"Existing numeric Rainbow selections must retain their pre-sevenfold meaning");
		assertEquals("middleworld", bindings.get("rainbow_crystal").get(6));
	}

	@Test
	void persistedModeValuesNormalizeAndWrap() {
		assertEquals(0, CrystalModeState.current(0, 3));
		assertEquals(2, CrystalModeState.current(-1, 3));
		assertEquals(1, CrystalModeState.advance(0, 3));
		assertEquals(2, CrystalModeState.advance(1, 3));
		assertEquals(0, CrystalModeState.advance(2, 3));
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

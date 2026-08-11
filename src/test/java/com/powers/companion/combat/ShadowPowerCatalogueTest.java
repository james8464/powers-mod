package com.powers.companion.combat;

import com.powers.power.PowerRegistry;
import com.powers.power.crystals.CrystalAbilityCatalog;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowPowerCatalogueTest {
	@BeforeAll
	static void initializePowers() {
		PowerRegistry.initialize();
	}

	@Test
	void manifestMatchesEveryInnateAndExactlyThreeSwordUniquesWithoutCrystals() {
		var ids = ShadowPowerCatalogue.actions().stream().map(ShadowPowerAction::id).toList();
		assertEquals(PowerRegistry.getAll().stream().map(power -> power.id().getPath()).toList(),
				ids.subList(0, 23));
		assertEquals(List.of("call_hollowed", "blight_ground", "nightfall_dominion"),
				ids.subList(23, 26));
		Set<String> crystals = CrystalAbilityCatalog.defaults().values().stream()
				.flatMap(List::stream).collect(Collectors.toSet());
		Set<String> innate = PowerRegistry.getAll().stream()
				.map(power -> power.id().getPath()).collect(Collectors.toSet());
		assertTrue(ids.stream().noneMatch(id -> crystals.contains(id) && !innate.contains(id)));
		ShadowPowerCatalogue.requireComplete();
	}

	@Test
	void everyActionHasAuthoredCombatMetadata() {
		for (ShadowPowerAction action : ShadowPowerCatalogue.actions()) {
			assertTrue(action.cost() >= 0);
			assertTrue(action.destructionTier() >= 0 && action.destructionTier() <= 10);
		}
	}
}

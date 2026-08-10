package com.powers.item;

import com.powers.power.crystals.CrystalAbilityCatalog;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowSwordCatalogueTest {
	@Test
	void exposesEveryInnateAndCrystalPowerPlusItsTwoUniqueCommands() {
		var actions = ShadowSwordCatalogue.definitions();
		Set<String> keys = new HashSet<>();
		actions.forEach(action -> assertTrue(keys.add(action.key()), action.key()));

		assertEquals(28, actions.stream().filter(action -> action.source()
				== ShadowSwordCatalogue.Source.INNATE).count());
		Set<String> crystalIds = new HashSet<>();
		CrystalAbilityCatalog.defaults().values().forEach(crystalIds::addAll);
		assertEquals(crystalIds,
				actions.stream().filter(action -> action.source() == ShadowSwordCatalogue.Source.CRYSTAL)
						.map(ShadowSwordCatalogue.Definition::abilityId).collect(java.util.stream.Collectors.toSet()));
		assertTrue(keys.contains("command/summon_darkness"));
		assertTrue(keys.contains("command/spread_darkness"));
		assertEquals(5, actions.stream()
				.filter(action -> action.source() == ShadowSwordCatalogue.Source.DARKNESS).count());
		assertEquals(Set.of(3, 5, 7, 9, 10), actions.stream()
				.filter(action -> action.source() == ShadowSwordCatalogue.Source.DARKNESS)
				.map(ShadowSwordCatalogue.Definition::requiredDarknessRank)
				.collect(java.util.stream.Collectors.toSet()));
	}
}

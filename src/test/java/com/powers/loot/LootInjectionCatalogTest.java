package com.powers.loot;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootInjectionCatalogTest {
	@Test
	void augmentsEveryFormerOverrideWithoutReplacingVanillaTables() {
		var groups = LootInjectionCatalog.groups();
		assertEquals(19, groups.size());
		var tables = new HashSet<String>();
		for (LootDropGroup group : groups) {
			assertTrue(tables.add(group.tableId()), group.tableId());
			assertTrue(group.chance() > 0 && group.chance() <= 1);
			assertTrue(group.itemIds().stream().allMatch(id -> id.startsWith("imported_")));
		}
	}
}

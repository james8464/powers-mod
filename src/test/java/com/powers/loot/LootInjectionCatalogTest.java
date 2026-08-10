package com.powers.loot;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootInjectionCatalogTest {
	@Test
	void augmentsEveryFormerOverrideWithoutReplacingVanillaTables() {
		var groups = LootInjectionCatalog.groups();
		assertTrue(groups.size() >= 28);
		var tables = new HashSet<String>();
		for (LootDropGroup group : groups) {
			assertTrue(tables.add(group.tableId()), group.tableId());
			assertTrue(group.chance() > 0 && group.chance() <= 1);
			assertTrue(group.itemIds().stream().allMatch(id -> id.startsWith("imported_")
					|| id.equals("minecraft:knowledge_book")));
		}
		assertTrue(groups.stream().filter(group -> group.tableId().equals(
				"minecraft:chests/stronghold_library")).flatMap(group -> group.itemIds().stream())
				.anyMatch("minecraft:knowledge_book"::equals));
		var obtainable = groups.stream().flatMap(group -> group.itemIds().stream()).toList();
		assertTrue(obtainable.contains("imported_artifact_lodestone"));
		assertTrue(obtainable.contains("imported_device_miniportal"));
		assertTrue(obtainable.contains("imported_artifact_philosopherstone"));
		assertTrue(obtainable.contains("imported_artifact_flute"));
		assertTrue(obtainable.contains("imported_artifact_soulmatrix"));
		assertTrue(obtainable.contains("imported_magic_essence_soul_dust"));
	}
}

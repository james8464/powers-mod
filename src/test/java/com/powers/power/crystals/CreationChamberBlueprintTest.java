package com.powers.power.crystals;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreationChamberBlueprintTest {
	@Test
	void chamberIsASmallUniqueAllowlistedServerTemplate() throws java.io.IOException {
		var json = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(
				java.nio.file.Path.of("src/main/resources/data/powers/creation_blueprints/orange_chamber.json")))
				.getAsJsonObject();
		var placements = CreationBlueprintParser.parse("powers:orange_chamber", json).blocks();
		assertEquals(26, placements.size());
		assertEquals(placements.size(), new HashSet<>(placements.stream()
				.map(CreationBlueprintDefinition.BlockEntry::offset).toList()).size());
		assertTrue(placements.stream().allMatch(placement ->
				java.util.Set.of("minecraft:orange_concrete", "minecraft:orange_stained_glass",
						"minecraft:glowstone").contains(placement.block().toString())));
	}
}

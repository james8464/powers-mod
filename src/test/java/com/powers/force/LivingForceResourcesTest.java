package com.powers.force;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the containment materials that spreading realm matter must preserve. */
class LivingForceResourcesTest {
	@Test
	void immunityTagContainsAmethystAndMindscapeLandmarks() throws IOException {
		Path path = Path.of(System.getProperty("user.dir"), "src/main/resources/data/powers/tags/block",
				"living_force_immune.json");
		JsonArray values = JsonParser.parseString(Files.readString(path))
				.getAsJsonObject().getAsJsonArray("values");
		Set<String> identifiers = new HashSet<>();
		values.forEach(value -> identifiers.add(value.getAsString()));

		for (String required : Set.of("#powers:amethyst", "minecraft:gold_block",
				"minecraft:crying_obsidian", "minecraft:end_rod",
				"minecraft:soul_lantern", "powers:light_memory_obelisk",
				"powers:dark_memory_obelisk")) {
			assertTrue(identifiers.contains(required), () -> "Missing force immunity: " + required);
		}
	}
}

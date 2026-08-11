package com.powers.entity;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the operator-facing spawn items needed for single-player power testing. */
class PlayerLikeSpawnEggResourcesTest {
	private static final Path ASSETS = Path.of("src/main/resources/assets/powers");

	@Test
	void everyPlayerLikeMobHasASelfContainedSpawnEggTexture() throws Exception {
		String language = Files.readString(ASSETS.resolve("lang/en_us.json"));
		for (String id : new String[] {"darkness_creature_spawn_egg", "power_test_actor_spawn_egg",
				"radiant_sentinel_spawn_egg", "first_vessel_spawn_egg"}) {
			Path definition = ASSETS.resolve("items/" + id + ".json");
			Path model = ASSETS.resolve("models/item/" + id + ".json");
			Path texture = ASSETS.resolve("textures/item/" + id + ".png");
			assertTrue(Files.isRegularFile(definition), id + " item definition");
			assertTrue(Files.isRegularFile(model), id + " model");
			assertTrue(Files.isRegularFile(texture), id + " texture");
			assertEquals("powers:item/" + id,
					JsonParser.parseString(Files.readString(definition)).getAsJsonObject()
							.getAsJsonObject("model").get("model").getAsString());
			assertEquals("minecraft:item/generated",
					JsonParser.parseString(Files.readString(model)).getAsJsonObject()
							.get("parent").getAsString());
			assertEquals("powers:item/" + id,
					JsonParser.parseString(Files.readString(model)).getAsJsonObject()
							.getAsJsonObject("textures").get("layer0").getAsString());
			assertTrue(language.contains("\"item.powers." + id + "\""), id + " translation");
		}
	}
}

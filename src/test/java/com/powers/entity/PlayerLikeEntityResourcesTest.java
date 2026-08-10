package com.powers.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerLikeEntityResourcesTest {
	private static final Path RESOURCES = Path.of(System.getProperty("user.dir"), "src/main/resources");

	@Test
	void darkRealmNaturallySpawnsTheDarknessCreature() throws IOException {
		JsonObject biome = JsonParser.parseString(Files.readString(RESOURCES.resolve(
				"data/powers/worldgen/biome/dark_realm.json"))).getAsJsonObject();
		boolean registered = biome.getAsJsonObject("spawners").getAsJsonArray("monster").asList()
				.stream().map(element -> element.getAsJsonObject().get("type").getAsString())
				.anyMatch("powers:darkness_creature"::equals);
		assertTrue(registered);
	}

	@Test
	void playerLikeSkinUsesTheVanillaSkinCanvas() throws IOException {
		byte[] png = Files.readAllBytes(RESOURCES.resolve("assets/powers/textures/entity/darkness_player.png"));
		assertEquals(64, readInt(png, 16));
		assertEquals(64, readInt(png, 20));
	}

	private static int readInt(byte[] bytes, int offset) {
		return (bytes[offset] & 0xFF) << 24 | (bytes[offset + 1] & 0xFF) << 16
				| (bytes[offset + 2] & 0xFF) << 8 | bytes[offset + 3] & 0xFF;
	}
}

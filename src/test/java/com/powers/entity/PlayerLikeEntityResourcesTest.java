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
	void everyPlayerLikeSkinUsesTheVanillaCanvasAndPaintsEveryBaseFace() throws IOException {
		for (String id : new String[] {"darkness_player", "test_actor", "radiant_sentinel",
				"first_vessel"}) {
			var image = javax.imageio.ImageIO.read(RESOURCES.resolve(
					"assets/powers/textures/entity/" + id + ".png").toFile());
			assertEquals(64, image.getWidth(), id + " width");
			assertEquals(64, image.getHeight(), id + " height");
			for (int[] point : new int[][] {{12, 12}, {24, 24}, {6, 24}, {46, 24},
					{22, 56}, {38, 56}}) {
				assertTrue((image.getRGB(point[0], point[1]) >>> 24) != 0,
						id + " left a transparent base UV face at " + point[0] + "," + point[1]);
			}
		}
	}
}

package com.powers.player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancementResourcesTest {
	private static final Path RESOURCES = Path.of(System.getProperty("user.dir"), "src/main/resources");

	@Test
	void progressionRootsUseVersionCorrectGuiSprites() throws IOException {
		assertBackground("skill_root.json", "powers:gui/advancements/backgrounds/radiant_path");
		assertBackground("darkness_root.json", "powers:gui/advancements/backgrounds/shadow_path");
	}

	@Test
	void darknessLevelsAreGrantedOnlyByThePersistentDeedTracker() throws IOException {
		for (int level = 1; level <= SkillSystem.DARKNESS_MAX_LEVEL; level++) {
			String file = "darkness/level_%02d.json".formatted(level);
			JsonObject advancement = json("data/powers/advancement/" + file);
			JsonObject criteria = advancement.getAsJsonObject("criteria");
			assertEquals(1, criteria.size(), file);
			assertEquals("minecraft:impossible",
					criteria.getAsJsonObject("deed").get("trigger").getAsString(), file);
		}
	}

	@Test
	void normalLevelsAreGrantedOnlyByThePersistentMasteryTracker() throws IOException {
		for (int level = 1; level <= SkillSystem.MAX_LEVEL; level++) {
			String file = "skill/level_%02d.json".formatted(level);
			JsonObject advancement = json("data/powers/advancement/" + file);
			JsonObject criteria = advancement.getAsJsonObject("criteria");
			assertEquals(1, criteria.size(), file);
			assertEquals("minecraft:impossible",
					criteria.getAsJsonObject("mastery").get("trigger").getAsString(), file);
		}
	}

	private static void assertBackground(String advancement, String sprite) throws IOException {
		JsonObject root = json("data/powers/advancement/" + advancement);
		assertEquals(sprite, root.getAsJsonObject("display").get("background").getAsString());

		String texture = sprite.substring("powers:".length()) + ".png";
		assertTrue(Files.isRegularFile(RESOURCES.resolve("assets/powers/textures").resolve(texture)), texture);
	}

	private static JsonObject json(String relative) throws IOException {
		return JsonParser.parseString(Files.readString(RESOURCES.resolve(relative))).getAsJsonObject();
	}
}

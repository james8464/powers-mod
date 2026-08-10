package com.powers.realm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmResourcesTest {
	private static final Path RESOURCES = Path.of(System.getProperty("user.dir"), "src/main/resources");

	@Test
	void lightRealmUsesAStaticWhiteOverworldSkyDisc() throws IOException {
		JsonObject type = json("data/powers/dimension_type/light_realm_type.json");
		JsonObject attributes = type.getAsJsonObject("attributes");

		assertEquals("overworld", type.get("skybox").getAsString());
		assertEquals("#ffffff", attributes.get("minecraft:visual/sky_color").getAsString());
		assertEquals("#ffffff", attributes.get("minecraft:visual/fog_color").getAsString());
		assertFalse(type.has("timelines"), "a day timeline would tint the white sky after dusk");
	}

	@Test
	void everyMemoryExplainsItsPathAndRewardInLanguage() throws IOException {
		JsonObject lang = json("assets/powers/lang/en_us.json");

		for (RealmKind kind : RealmKind.values()) {
			for (MemorySite site : RealmLayout.sites(kind)) {
				assertTrue(lang.has(site.memoryKey()), site.memoryKey());
				assertTrue(lang.has(site.pathKey()), site.pathKey());
			}
		}
		assertTrue(lang.has("realm.powers.path_offer"));
		assertTrue(lang.has("realm.powers.energy_restored"));
	}

	private static JsonObject json(String relative) throws IOException {
		return JsonParser.parseString(Files.readString(RESOURCES.resolve(relative))).getAsJsonObject();
	}
}

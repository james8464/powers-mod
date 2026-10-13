package com.powers.loot;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmHeraldLootResourcesTest {
	@Test
	void eachHeraldKeepsItsGuaranteedMythicReward() throws Exception {
		assertTrue(isGuaranteed("dark_herald", "powers:lycanbane"));
		assertTrue(isGuaranteed("light_herald", "powers:heavenly_partisan"));
	}

	private static boolean isGuaranteed(String tableName, String itemId) throws Exception {
		Path path = Path.of("src/main/resources/data/powers/loot_table/entities/"
				+ tableName + ".json");
		var pools = JsonParser.parseString(Files.readString(path)).getAsJsonObject()
				.getAsJsonArray("pools");
		for (var poolElement : pools) {
			var pool = poolElement.getAsJsonObject();
			if (pool.has("conditions") || pool.get("rolls").getAsInt() < 1
					|| pool.getAsJsonArray("entries").size() != 1) continue;
			var entry = pool.getAsJsonArray("entries").get(0).getAsJsonObject();
			if (itemId.equals(entry.has("name") ? entry.get("name").getAsString() : "")) {
				return true;
			}
		}
		return false;
	}
}

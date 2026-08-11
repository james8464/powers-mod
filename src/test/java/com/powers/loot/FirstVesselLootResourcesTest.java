package com.powers.loot;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FirstVesselLootResourcesTest {
	@Test
	void firstVesselAlwaysDropsAMiniportal() throws Exception {
		Path path = Path.of("src/main/resources/data/powers/loot_table/entities/first_vessel.json");
		var pools = JsonParser.parseString(Files.readString(path)).getAsJsonObject()
				.getAsJsonArray("pools");
		boolean guaranteed = false;
		for (var poolElement : pools) {
			var pool = poolElement.getAsJsonObject();
			if (pool.has("conditions")) continue;
			for (var entryElement : pool.getAsJsonArray("entries")) {
				var entry = entryElement.getAsJsonObject();
				if ("powers:imported_device_miniportal".equals(
						entry.has("name") ? entry.get("name").getAsString() : "")) {
					guaranteed = pool.get("rolls").getAsInt() >= 1
							&& pool.getAsJsonArray("entries").size() == 1;
				}
			}
		}
		assertTrue(guaranteed);
	}
}

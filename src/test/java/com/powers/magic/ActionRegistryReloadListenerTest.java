package com.powers.magic;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActionRegistryReloadListenerTest {
	@Test
	void datapackDocumentsMergeInStableResourceOrder() {
		Map<String, String> aliases = ActionRegistryReloadListener.parseDocuments(List.of(
				new ActionRegistryReloadListener.Document("powers:first",
						JsonParser.parseString("{\"aliases\":{\"old_fire\":\"fireball\"}}").getAsJsonObject()),
				new ActionRegistryReloadListener.Document("addon:second",
						JsonParser.parseString("{\"aliases\":{\"retired_storm\":\"old_fire\"}}").getAsJsonObject())));

		assertEquals(Map.of("old_fire", "fireball", "retired_storm", "old_fire"), aliases);
	}

	@Test
	void duplicateAliasAcrossPacksRejectsEntirePreparedReload() {
		List<ActionRegistryReloadListener.Document> documents = List.of(
				new ActionRegistryReloadListener.Document("a:first",
						JsonParser.parseString("{\"aliases\":{\"retired\":\"fireball\"}}").getAsJsonObject()),
				new ActionRegistryReloadListener.Document("b:second",
						JsonParser.parseString("{\"aliases\":{\"retired\":\"lightning_strike\"}}").getAsJsonObject()));

		assertThrows(IllegalArgumentException.class,
				() -> ActionRegistryReloadListener.parseDocuments(documents));
	}
}

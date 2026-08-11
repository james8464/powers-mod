package com.powers.item;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ImportedItemNamesTest {
	@Test
	void visibleNamesDoNotExposeAssetFolderPrefixes() throws Exception {
		var language = JsonParser.parseString(Files.readString(Path.of(
				"src/main/resources/assets/powers/lang/en_us.json"))).getAsJsonObject();
		for (var entry : language.entrySet()) {
			if (!entry.getKey().startsWith("item.powers.imported_")) continue;
			String name = entry.getValue().getAsString();
			assertFalse(name.matches("^(Artifact|Food|Book|Device|Magic Essence)\\b.*"),
					entry.getKey() + " = " + name);
		}
	}
}

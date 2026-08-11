package com.powers.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Executable resource and documentation contract for every registered item row. */
class ItemCatalogueExecutableAuditTest {
	private static final Path ROOT = Path.of(System.getProperty("user.dir"));

	@Test
	void all262RowsHaveUniqueRegistryModelTranslationPurposeAndAcquisitionContracts() throws Exception {
		List<String> rows = Files.readAllLines(ROOT.resolve("docs/gameplay/item-catalogue.md")).stream()
				.filter(line -> line.startsWith("| `powers:"))
				.toList();
		assertEquals(262, rows.size(), "catalogue row count drifted from the live registry contract");
		JsonObject language = JsonParser.parseString(Files.readString(ROOT.resolve(
				"src/main/resources/assets/powers/lang/en_us.json"))).getAsJsonObject();
		Set<String> ids = new HashSet<>();
		for (String row : rows) {
			String[] columns = row.split("\\|", -1);
			assertEquals(7, columns.length, row);
			String id = columns[1].strip().replace("`", "");
			String path = id.substring("powers:".length());
			assertTrue(ids.add(id), "duplicate catalogue row " + id);
			assertFalse(columns[2].isBlank(), "missing translated name for " + id);
			assertFalse(columns[3].isBlank(), "missing family for " + id);
			assertFalse(columns[4].isBlank() || columns[4].matches("(?i).*\\b(?:tbd|unknown)\\b.*"),
					"missing purpose for " + id);
			assertFalse(columns[5].isBlank() || columns[5].matches("(?i).*\\b(?:tbd|unknown)\\b.*"),
					"missing acquisition status for " + id);
			assertTrue(Files.isRegularFile(ROOT.resolve(
					"src/main/resources/assets/powers/items/" + path + ".json")),
					"missing item definition for " + id);
			assertTrue(Files.isRegularFile(ROOT.resolve(
					"src/main/resources/assets/powers/models/item/" + path + ".json")),
					"missing item model for " + id);
			boolean translated = language.has("item.powers." + path)
					|| language.has("block.powers." + path);
			assertTrue(translated, "missing en_us translation for " + id);
		}
	}
}

package com.powers.knowledge;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GeneratedKnowledgeCoverageTest {
	private static final Path ROOT = Path.of(System.getProperty("user.dir"));
	private static final Path GENERATED = ROOT.resolve(
			"src/main/resources/data/powers/knowledge_entries/generated");

	@Test
	void everyGeneratedCatalogueEntryParsesAndIsIndividuallyRetrievable() throws IOException {
		List<Path> files;
		try (var paths = Files.walk(GENERATED)) {
			files = paths.filter(path -> path.toString().endsWith(".json"))
					.sorted(Comparator.comparing(Path::toString)).toList();
		}
		assertEquals(435, files.size());

		List<KnowledgeEntry> entries = new ArrayList<>(files.size());
		for (Path file : files) {
			String relative = GENERATED.relativize(file).toString().replace('\\', '/');
			String id = "powers:generated/" + relative.substring(0, relative.length() - 5);
			try (Reader reader = Files.newBufferedReader(file)) {
				KnowledgeEntry parsed = KnowledgeEntryParser.parse(id,
						JsonParser.parseReader(reader).getAsJsonObject());
				assertFalse(parsed.sources().isEmpty());
				entries.add(parsed);
			}
		}

		KnowledgeIndex index = new KnowledgeIndex(entries);
		for (KnowledgeEntry entry : entries) {
			String lookup = entry.keywords().stream()
					.filter(keyword -> keyword.startsWith("knowledge_lookup_")).findFirst().orElseThrow();
			assertEquals(entry.id(), index.answer(lookup, 10).entryId(),
					() -> "Generated entry is not independently retrievable: " + entry.id());
		}
	}
}

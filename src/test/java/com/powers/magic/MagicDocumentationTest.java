package com.powers.magic;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MagicDocumentationTest {
	private static final Path ROOT = Path.of("").toAbsolutePath();

	@Test
	void committedDocumentsExactlyMatchTheCatalogueAndResolver() throws IOException {
		assertEquals(MagicDocumentation.renderCatalogue(), read("docs/interactions/action-catalogue.md"));
		assertEquals(MagicDocumentation.renderRules(), read("docs/interactions/interaction-rules.md"));
		String matrix = read("docs/interactions/interaction-matrix.csv");
		assertEquals(2_146, matrix.lines().count());
		assertEquals(MagicDocumentation.renderMatrix(), matrix);
	}

	private static String read(String relative) throws IOException {
		return Files.readString(ROOT.resolve(relative));
	}
}

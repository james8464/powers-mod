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
		String catalogue = read("docs/interactions/action-catalogue.md");
		assertEquals(MagicDocumentation.renderCatalogue(), catalogue);
		org.junit.jupiter.api.Assertions.assertTrue(catalogue.contains("| Significance | Generic beats |"));
		assertEquals(MagicDocumentation.renderRules(), read("docs/interactions/interaction-rules.md"));
		String matrix = read("docs/interactions/interaction-matrix.csv");
		assertEquals(2_702, matrix.lines().count());
		assertEquals(MagicDocumentation.renderMatrix(), matrix);
	}

	private static String read(String relative) throws IOException {
		return Files.readString(ROOT.resolve(relative));
	}
}

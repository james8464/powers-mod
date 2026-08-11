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
		long actionCount = MagicActionCatalogue.defaults().definitions().size();
		assertEquals(1L + actionCount * (actionCount + 1L) / 2L, matrix.lines().count());
		assertEquals(MagicDocumentation.renderMatrix(), matrix);
		String lifecycle = read("docs/interactions/lifecycle-matrix.csv");
		long lifecycleCases = (long) com.powers.magic.runtime.MagicLifecycleRules.Form.values().length
				* com.powers.magic.runtime.MagicLifecycleRules.Source.values().length
				* com.powers.magic.runtime.MagicLifecycleRules.Event.values().length;
		assertEquals(1L + lifecycleCases, lifecycle.lines().count());
		assertEquals(MagicDocumentation.renderLifecycleMatrix(), lifecycle);
	}

	private static String read(String relative) throws IOException {
		return Files.readString(ROOT.resolve(relative));
	}
}

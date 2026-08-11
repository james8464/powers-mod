package com.powers.quality;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Validates README facts against the packaged registry resources and repository files. */
class ReadmeContractTest {
	private static final Path ROOT = Path.of(System.getProperty("user.dir"));
	private static final Pattern REGISTRY_TOTAL = Pattern.compile(
			"The registry currently contains ([0-9]+) gameplay/block rows");
	private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[[^]]+]\\(([^)]+)\\)");

	@Test
	void registryTotalMatchesAuthoritativeItemDefinitions() throws Exception {
		String readme = Files.readString(ROOT.resolve("README.md"));
		var match = REGISTRY_TOTAL.matcher(readme);
		assertTrue(match.find(), "README must state the gameplay/block registry total");
		long definitions;
		try (var paths = Files.list(ROOT.resolve("src/main/resources/assets/powers/items"))) {
			definitions = paths.filter(path -> path.getFileName().toString().endsWith(".json")).count();
		}
		assertEquals(definitions, Long.parseLong(match.group(1)),
				"README registry total drifted from packaged item definitions");
	}

	@Test
	void everyLocalMarkdownLinkResolves() throws Exception {
		var links = MARKDOWN_LINK.matcher(Files.readString(ROOT.resolve("README.md")));
		int checked = 0;
		while (links.find()) {
			String target = links.group(1);
			if (target.startsWith("http://") || target.startsWith("https://")
					|| target.startsWith("mailto:") || target.startsWith("#")) continue;
			String path = target.split("#", 2)[0];
			assertTrue(Files.isRegularFile(ROOT.resolve(path)), () -> "Broken README link: " + target);
			checked++;
		}
		assertTrue(checked > 0, "README must retain locally validated documentation links");
	}
}

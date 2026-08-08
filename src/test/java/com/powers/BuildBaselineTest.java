package com.powers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

class BuildBaselineTest {
	@Test
	void exposesTheRegisteredModNamespace() {
		assertEquals("powers", PowersMod.MOD_ID);
	}

	@Test
	void everyTrackedNonItemAssetHasAnExactAuditRow() throws IOException {
		Path root = Path.of(System.getProperty("user.dir"));
		Path assets = root.resolve("src/main/resources/assets/powers");
		Set<String> tracked;
		try (var paths = Files.walk(assets)) {
			tracked = paths.filter(Files::isRegularFile)
					.map(assets::relativize)
					.map(path -> path.toString().replace('\\', '/'))
					.filter(path -> !path.startsWith("items/")
							&& !path.startsWith("models/item/")
							&& !path.startsWith("textures/item/"))
					.collect(java.util.stream.Collectors.toUnmodifiableSet());
		}
		String manifest = Files.readString(root.resolve("docs/quality/asset-audit.md"));
		for (String relative : tracked) {
			assertTrue(manifest.contains("| `" + relative + "` |"), () -> "Missing asset audit row: " + relative);
		}
		long rows = manifest.lines().filter(line -> line.startsWith("| `")).count();
		assertEquals(tracked.size(), rows);
	}
}

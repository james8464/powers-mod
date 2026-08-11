package com.powers.quality;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps the packaged Fabric version synchronized with the current changelog release. */
class ReleaseMetadataTest {
	private static final Pattern RELEASE = Pattern.compile("(?m)^## ([0-9]+\\.[0-9]+\\.[0-9]+) - ");

	@Test
	void packagedVersionMatchesTheNewestChangelogRelease() throws IOException {
		Path project = Path.of(System.getProperty("user.dir"));
		Properties properties = new Properties();
		try (var input = Files.newInputStream(project.resolve("gradle.properties"))) {
			properties.load(input);
		}
		Matcher release = RELEASE.matcher(Files.readString(project.resolve("CHANGELOG.md")));
		assertTrue(release.find(), "CHANGELOG must begin with a semantic release heading");
		assertEquals(release.group(1), properties.getProperty("mod_version"));
	}
}

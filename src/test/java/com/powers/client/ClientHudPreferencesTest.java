package com.powers.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ClientHudPreferencesTest {
	@TempDir Path directory;

	@Test
	void loadingExistingSharedConfigPreservesAudioComfortField() throws Exception {
		Path config = directory.resolve("powers-client.json");
		String original = """
				{
				  "anchor": "TOP_LEFT",
				  "horizontalMargin": 7,
				  "verticalMargin": 9,
				  "powerRailMargin": 11,
				  "reducedTinnitus": true
				}
				""";
		Files.writeString(config, original);

		ClientHudPreferences.load(config);

		assertEquals(original, Files.readString(config));
		assertEquals(7, ClientHudPreferences.get().horizontalMargin());
	}
}

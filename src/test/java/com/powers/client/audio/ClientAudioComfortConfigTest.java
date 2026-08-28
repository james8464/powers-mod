package com.powers.client.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientAudioComfortConfigTest {
	@TempDir Path directory;

	@Test
	void absentMalformedAndOversizedFilesDefaultToOrdinaryAudio() throws Exception {
		assertFalse(ClientAudioComfortConfig.read(directory.resolve("absent.json")));
		Path malformed = directory.resolve("malformed.json");
		Files.writeString(malformed, "{not-json");
		assertFalse(ClientAudioComfortConfig.read(malformed));
		Path oversized = directory.resolve("oversized.json");
		Files.writeString(oversized, " ".repeat(4_097));
		assertFalse(ClientAudioComfortConfig.read(oversized));
	}

	@Test
	void onlyAnExplicitBooleanEnablesReducedTinnitus() throws Exception {
		Path enabled = directory.resolve("enabled.json");
		Files.writeString(enabled, """
				{"reducedTinnitus":true,"unknown":"ignored"}
				""");
		assertTrue(ClientAudioComfortConfig.read(enabled));

		Path wrongType = directory.resolve("wrong-type.json");
		Files.writeString(wrongType, "{\"reducedTinnitus\":\"true\"}");
		assertFalse(ClientAudioComfortConfig.read(wrongType));
	}
}

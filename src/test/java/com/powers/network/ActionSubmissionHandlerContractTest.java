package com.powers.network;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActionSubmissionHandlerContractTest {
	@Test
	void everyRevisionedHandlerUsesTheSharedPreSideEffectService() throws IOException {
		Path root = Path.of(System.getProperty("user.dir"), "src/main/java/com/powers/network");

		assertEquals(5, occurrences(Files.readString(root.resolve("ShadowSwordPackets.java")),
				"ActionSubmissionService.submit("), "select/commit/cycle/bind/teleport");
		assertEquals(1, occurrences(Files.readString(root.resolve("GrimoirePackets.java")),
				"ActionSubmissionService.submit("), "grimoire select");
		assertEquals(1, occurrences(Files.readString(root.resolve("CrystalSelectorPackets.java")),
				"ActionSubmissionService.submit("), "crystal select");
	}

	private static int occurrences(String source, String needle) {
		return (source.length() - source.replace(needle, "").length()) / needle.length();
	}
}

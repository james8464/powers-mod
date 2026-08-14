package com.powers.command;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RestartSoakCommandContractTest {
	@Test
	void operatorTreeExposesEveryRestartSoakLifecyclePhase() throws IOException {
		String source = Files.readString(Path.of(
				"src/main/java/com/powers/command/TestingCommand.java"));
		assertTrue(source.contains("Commands.literal(\"soak\")"));
		assertTrue(source.contains("soakPhase(\"verify\", SoakPhase.VERIFY)"));
		assertTrue(source.contains("soakPhase(\"seed\", SoakPhase.SEED)"));
		assertTrue(source.contains("soakPhase(\"status\", SoakPhase.STATUS)"));
		assertTrue(source.contains("soakPhase(\"rollover\", SoakPhase.ROLLOVER)"));
		assertTrue(source.contains("RestartSoakScenario.CLIENT_NAME"));
		assertTrue(source.contains("POWERS_SOAK_"));
	}
}

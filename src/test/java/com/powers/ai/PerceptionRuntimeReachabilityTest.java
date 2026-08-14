package com.powers.ai;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PerceptionRuntimeReachabilityTest {
	@Test
	void shadowAndBothGuardianFamiliesShareTheSnapshotService() throws Exception {
		assertTrue(source("companion/combat/ShadowCombatController.java")
				.contains("PerceptionSnapshotService.observe"));
		assertTrue(source("entity/GuardianAlignmentField.java")
				.contains("PerceptionSnapshotService.observe"));
		assertTrue(source("entity/DarknessCreature.java")
				.contains("GuardianPerceptionTargetGoal"));
		assertTrue(source("entity/RadiantSentinel.java")
				.contains("GuardianPerceptionTargetGoal"));
	}

	@Test
	void serverStopClearsSnapshotsAndDiagnosticsExposeTheirCost() throws Exception {
		assertTrue(source("PowersServerLifecycle.java")
				.contains("PerceptionSnapshotService.clear()"));
		assertTrue(source("command/PowerDiagnosticsCommand.java")
				.contains("PerceptionSnapshotService.diagnostics()"));
	}

	private static String source(String relative) throws Exception {
		return Files.readString(Path.of("src/main/java/com/powers").resolve(relative));
	}
}

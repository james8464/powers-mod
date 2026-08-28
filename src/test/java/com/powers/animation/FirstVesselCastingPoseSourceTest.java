package com.powers.animation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class FirstVesselCastingPoseSourceTest {
	@Test
	void poseEmissionIsGuardedByActualActionCommit() throws IOException {
		String combat = Files.readString(Path.of(
				"src/main/java/com/powers/boss/FirstVesselCombat.java"));
		assertTrue(combat.contains("boolean committed = switch (action.powerId())"));
		assertTrue(combat.contains("if (committed) {\n\t\t\tCastingPoseService.start"));
		assertTrue(combat.contains("private static boolean rush("));
		assertTrue(combat.contains("private static boolean step("));
	}
}

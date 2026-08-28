package com.powers.animation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class CastingPoseAcceptanceSourceTest {
	@Test
	void clientAcceptanceInjectsRealDelayAndCapturesActualWalking() throws IOException {
		String acceptance = Files.readString(Path.of(
				"src/gametest/java/com/powers/gametest/CastingPoseClientAcceptance.java"));
		assertTrue(acceptance.contains("PacketFaultProfile.named(\"delay300\""));
		assertTrue(acceptance.contains("captureLocomotionWalk("));
		assertTrue(acceptance.contains("entity.walkAnimation.speed(1.0F)"));
		assertTrue(acceptance.contains("movementDistance < 0.4"));
		assertTrue(acceptance.contains("observedWalkSpeed <= 0.05"));
		assertTrue(!acceptance.contains("walkAnimation.update("));
	}
}

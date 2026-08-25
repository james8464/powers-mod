package com.powers.fx;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Guards the dedicated VFX-004 real-client acceptance fixture against model-only substitution. */
class VisualScarFaultAcceptanceSourceTest {
	@Test
	void dedicatedFixtureUsesProductionScarAndFaultPaths() throws IOException {
		String source = Files.readString(Path.of(
				"src/gametest/java/com/powers/client/VisualScarFaultAcceptanceClientGameTests.java"));

		assertTrue(source.contains("implements FabricClientGameTest"));
		assertTrue(source.contains("VisualScarService.request"));
		assertTrue(source.contains("PacketFaultController.configureScoped"));
		assertTrue(source.contains("PacketFaultProfile.named(\"loss1\""));
		assertTrue(source.contains("PacketFaultProfile.named(\"loss5\""));
		assertTrue(source.contains("ClientVisualScarManager.entries()"));
		assertTrue(source.contains("PowersPlayNetworking.sendGuarded"));
		assertFalse(source.contains("new ClientVisualScarState("));
		assertFalse(source.contains("new MagicFxPackets.ScarFxPayload("));
		String build = Files.readString(Path.of("build.gradle"));
		assertTrue(build.contains("vfx004FaultClientOnly"));
		assertTrue(build.contains("com.powers.client.VisualScarFaultAcceptanceClientGameTests"));
	}
}

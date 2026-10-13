package com.powers.power.crystals;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CrystalDiscoveryProductionBoundaryTest {
	@Test
	void selectorKeepsUnknownControlsSelectableWhileHidingTheirIdentity() throws Exception {
		Path root = Path.of(System.getProperty("user.dir"));
		String screen = Files.readString(root.resolve(
				"src/client/java/com/powers/client/screen/RainbowConvergenceScreen.java"));
		assertTrue(screen.contains("discovered.contains(abilityId) ? abilityId : \"unknown\""));
		assertTrue(screen.contains("ignored -> choose(target)"),
				"unknown modes must remain keyboard/mouse selectable");
		String attachment = Files.readString(root.resolve(
				"src/main/java/com/powers/player/PlayerPowerAttachments.java"));
		assertTrue(attachment.contains("CRYSTAL_DISCOVERIES = persistentMap"));
	}
}

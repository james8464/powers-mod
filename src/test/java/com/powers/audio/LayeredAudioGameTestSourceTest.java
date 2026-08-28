package com.powers.audio;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class LayeredAudioGameTestSourceTest {
	@Test
	void mutationAcceptanceSnapshotsRealIsolatedActorEnergyHealthAndWorldState() throws Exception {
		String source = Files.readString(Path.of(
				"src/gametest/java/com/powers/gametest/LayeredAudioGameTests.java"));
		assertTrue(source.contains("PowersEntities.POWER_TEST_ACTOR"));
		assertTrue(source.contains("TestActorPowerState.energy"));
		assertTrue(source.contains("actor.getHealth()"));
		assertTrue(source.contains("helper.getBlockState"));
		assertTrue(source.contains("getPlayerList().getPlayer(playerId) == null"));
	}
}

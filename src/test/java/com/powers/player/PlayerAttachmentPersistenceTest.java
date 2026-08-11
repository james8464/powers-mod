package com.powers.player;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the save boundary between durable character data and runtime-only abilities. */
class PlayerAttachmentPersistenceTest {
	@Test
	void togglesAndFlightSnapshotsAreSessionOnly() throws IOException {
		String source = Files.readString(Path.of(System.getProperty("user.dir"),
				"src/main/java/com/powers/player/PlayerPowerAttachments.java"));

		assertTrue(source.contains("ACTIVE_TOGGLES = sessionStringList(\"active_toggles\")"));
		assertTrue(source.contains("FLIGHT_SNAPSHOT = sessionInt(\"flight_snapshot\", -1)"));
		assertFalse(source.contains("ACTIVE_TOGGLES = persistentStringList"));
		assertFalse(source.contains("FLIGHT_SNAPSHOT = persistentInt"));
	}

	@Test
	void durableCharacterSystemsRemainPersistent() throws IOException {
		String source = Files.readString(Path.of(System.getProperty("user.dir"),
				"src/main/java/com/powers/player/PlayerPowerAttachments.java"));

		for (String name : new String[] {
				"COOLDOWNS", "SKILL_LEVEL", "DARKNESS_LEVEL", "TELEPORT_CONSENT",
				"SHADOW_SWORD_FAVOURITES", "HEAVENLY_PARTISAN_FAVOURITES", "LAST_DEATH"
		}) {
			assertTrue(source.matches("(?s).*" + name + ".*persistent.*"), name);
		}
	}
}

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
				"SHADOW_SWORD_FAVOURITES", "HEAVENLY_PARTISAN_FAVOURITES",
				"SHADOW_SWORD_RECENTS", "HEAVENLY_PARTISAN_RECENTS", "LAST_DEATH"
		}) {
			assertTrue(source.matches("(?s).*" + name + ".*persistent.*"), name);
		}
	}

	@Test
	void attachmentSchemaRegistersDuringModBootstrapBeforePlayersDecode() throws IOException {
		Path root = Path.of(System.getProperty("user.dir"));
		String bootstrap = Files.readString(root.resolve(
				"src/main/java/com/powers/PowersBootstrap.java"));
		String schema = Files.readString(root.resolve(
				"src/main/java/com/powers/player/PlayerPowerSchema.java"));

		assertTrue(bootstrap.contains("PlayerPowerSchema.initialize()"),
				"persistent attachment types must exist before the first player save is decoded");
		assertTrue(schema.contains("PlayerPowerAttachments.initialize()"),
				"the schema bootstrap must force every attachment registration to load");
	}
}

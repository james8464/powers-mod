package com.powers.quality;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Static inventory for every production callback that outlives its initiating tick. */
class DelayedCallbackOwnershipTest {
	@Test
	void allScheduledMagicUsesOwnedStableDescriptors() throws Exception {
		String sources;
		try (var paths = Files.walk(Path.of("src/main/java"))) {
			sources = paths.filter(path -> path.toString().endsWith(".java"))
					.map(DelayedCallbackOwnershipTest::read)
					.reduce("", (left, right) -> left + "\n" + right);
		}

		for (String purpose : List.of("godly_punishment_followup", "fatal_body_death",
				"realm_confinement_retry", "restart_soak_settle", "travel_timeout",
				"travel_ticket_release", "locator_swell", "locator_heavens", "locator_reveal",
				"mindscape_commit", "marking_teleport", "teleport_commit",
				"teleport_storm_finish")) {
			assertTrue(sources.contains('"' + purpose + '"'), () -> "Missing delayed owner: " + purpose);
		}
		assertFalse(sources.contains("scheduleDelayed(\n\t\t\tMinecraftServer server, int ticks, Runnable"));
		assertFalse(read(Path.of("src/main/java/com/powers/power/AsyncAbilityTransaction.java"))
				.contains("private final MinecraftServer server"));
		String travel = read(Path.of("src/main/java/com/powers/power/travel/TravelChunkLoader.java"));
		assertFalse(travel.contains("private final ServerLevel level"));
		assertFalse(travel.contains("static MinecraftServer activeServer"),
				"Asynchronous travel must resolve the current server through its lifecycle epoch");
		try (var paths = Files.walk(Path.of("src/main/java/com/powers/network"))) {
			String network = paths.filter(path -> path.toString().endsWith(".java"))
					.map(DelayedCallbackOwnershipTest::read)
					.reduce("", (left, right) -> left + '\n' + right);
			assertFalse(network.contains("context.server().execute"),
					"Queued network work must retain a UUID, not the Fabric player context");
		}
	}

	private static String read(Path path) {
		try {
			return Files.readString(path);
		} catch (java.io.IOException failure) {
			throw new java.io.UncheckedIOException(failure);
		}
	}
}

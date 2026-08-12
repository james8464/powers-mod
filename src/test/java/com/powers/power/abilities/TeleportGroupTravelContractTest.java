package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeleportGroupTravelContractTest {
	@Test
	void coordinateActivationDoesNotConsultTeleportConsent() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/powers/power/AbilityActivationService.java"));
		String innate = method(source, "public static Result activateTeleport", "/** Completes artifact");
		String artifact = method(source, "public static Result activateArtifactTeleport", "private static Result cast");
		assertFalse(innate.contains("mayForceMove"));
		assertFalse(artifact.contains("mayForceMove"));
	}

	@Test
	void timeShiftUsesSharedCohortAndExactCoordinates() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/powers/power/abilities/TeleportAbility.java"));
		assertTrue(source.contains("TravelCohort.capture"));
		assertTrue(source.contains("TravelCohort.move"));
		assertTrue(source.contains("SafeDestinationResolver.validateExact"));
		assertFalse(source.contains("findSafeMarkSpot"));
		assertFalse(source.contains("x + 0.5"));
	}

	private static String method(String source, String start, String end) {
		int from = source.indexOf(start);
		int to = source.indexOf(end, from);
		return source.substring(from, to);
	}
}

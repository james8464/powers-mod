package com.powers.power.crystals;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MindscapeGroupTravelContractTest {
	@Test
	void opposedAndMiddleworldCrystalsUseOneAutomaticLivingCohort() throws Exception {
		String opposed = Files.readString(Path.of(
				"src/main/java/com/powers/power/crystals/MindscapeCrystalAbility.java"));
		String middle = Files.readString(Path.of(
				"src/main/java/com/powers/power/crystals/MiddleworldAbility.java"));
		assertTrue(opposed.contains("TravelCohort.capture"));
		assertTrue(opposed.contains("List<LivingEntity>"));
		assertTrue(opposed.contains("MindscapeMobReturnTracker"));
		assertFalse(opposed.contains("mayForceMove"));
		assertFalse(opposed.contains("journeyTarget"));
		assertTrue(middle.contains("extends MindscapeCrystalAbility"));
	}
}

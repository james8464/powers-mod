package com.powers.thermal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ThermalIntegrationContractTest {
	@Test
	void everyAuthoredThermalPowerUsesTheSharedPlanner() throws IOException {
		assertUsesSharedPlanner("power/abilities/IceManipulationAbility.java");
		assertUsesSharedPlanner("power/abilities/FireballImpactResolver.java");
		assertUsesSharedPlanner("power/crystals/InfernoAbility.java");
		assertUsesSharedPlanner("power/abilities/PlantHealingAbility.java");
	}

	private static void assertUsesSharedPlanner(String relativePath) throws IOException {
		String source = Files.readString(Path.of("src/main/java/com/powers").resolve(relativePath));
		assertTrue(source.contains("ThermalWorldService"),
				relativePath + " bypasses the shared thermal planner");
	}
}

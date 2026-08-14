package com.powers.command;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerTravelCommandContractTest {
	@Test
	void dimensionOnlyTravelLoadsItsArrivalAndNeverUsesTheRetiredVoidHeight() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/powers/command/PowerTravelCommand.java"));

		assertTrue(source.contains("TravelChunkLoader.request"));
		assertTrue(source.contains("RealmTerrain.arrivalY"));
		assertFalse(source.contains("-58.0"));
	}
}

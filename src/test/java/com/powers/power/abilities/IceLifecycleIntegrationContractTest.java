package com.powers.power.abilities;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class IceLifecycleIntegrationContractTest {
	@Test
	void bridgeBrittleAndFireLifecycleReachProductionOwners() throws IOException {
		String thermal = source("thermal/ThermalWorldService.java");
		String ice = source("power/abilities/IceManipulationAbility.java");
		String combat = source("PowerCombatEvents.java");
		String runtime = source("power/PowerAbilityRuntime.java");
		assertTrue(thermal.contains("Blocks.FROSTED_ICE.defaultBlockState()"));
		assertTrue(ice.contains("BrittleArmorManager.apply"));
		assertTrue(combat.contains("BrittleArmorManager.breakWithFire"));
		assertTrue(runtime.contains("BrittleArmorManager.tick"));
		assertTrue(runtime.contains("BrittleArmorManager.clearAll"));
	}

	private static String source(String relative) throws IOException {
		return Files.readString(Path.of("src/main/java/com/powers").resolve(relative));
	}
}

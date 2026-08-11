package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prevents forced-movement powers from bypassing the shared boss outcome vocabulary. */
class ControlResistanceCoverageTest {
	@Test
	void everyInnateControlImplementationUsesTheSharedOutcome() throws Exception {
		Path root = Path.of(System.getProperty("user.dir"), "src/main/java/com/powers/power/abilities");
		for (String file : List.of("TelekinesisAbility.java", "GravityDisplacementAbility.java",
				"BreezyBashAbility.java", "ThunderclapAbility.java", "EnergyBeamAbility.java",
				"StarfallImpactResolver.java", "SpeedBurstAbility.java", "SuperSpeedAbility.java",
				"DimensionalAnchorAbility.java")) {
			assertTrue(Files.readString(root.resolve(file)).contains("ControlResistance."), file);
		}
	}
}

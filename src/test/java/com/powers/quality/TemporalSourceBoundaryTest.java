package com.powers.quality;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalSourceBoundaryTest {
	private static final Path ROOT = Path.of("").toAbsolutePath();

	@Test
	void liveAcceptanceFixtureIsRegisteredAndExercisesTheProductionClock() throws Exception {
		Path source = ROOT.resolve(
				"src/gametest/java/com/powers/gametest/TemporalOwnershipGameTests.java");
		assertTrue(Files.isRegularFile(source), "INT-008 live temporal GameTests are missing");
		String tests = Files.readString(source);
		assertTrue(tests.contains("GlobalTimeStopManager.startCrystal"));
		assertTrue(tests.contains("tickRateManager().setFrozen"));
		assertTrue(tests.contains("Projectile"));
		assertTrue(tests.contains("SpellCastingManager"));
		assertTrue(tests.contains("CelestialRuinManager"));
		assertTrue(tests.contains("RealmEventManager"));

		String metadata = Files.readString(ROOT.resolve("src/gametest/resources/fabric.mod.json"));
		assertTrue(metadata.contains("com.powers.gametest.TemporalOwnershipGameTests"));
	}

	@Test
	void worldOwnedManagersCannotRegressToOwnershipOnlyFreezeChecks() throws Exception {
		for (String relative : new String[] {
				"src/main/java/com/powers/spell/SpellCastingManager.java",
				"src/main/java/com/powers/spell/SpellFieldManager.java",
				"src/main/java/com/powers/spell/CelestialRuinManager.java",
				"src/main/java/com/powers/realm/RealmEventManager.java",
				"src/main/java/com/powers/realm/RealmHeraldManager.java"
		}) {
			String source = Files.readString(ROOT.resolve(relative));
			assertTrue(source.contains("TemporalClocks"), relative + " bypasses the typed clock adapter");
			assertFalse(source.contains("GlobalTimeStopManager.isStopped"),
					relative + " still depends on POWERS ownership instead of vanilla freeze state");
		}
	}
}

package com.powers.magic.runtime;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the production entry points that must never regress to ad-hoc billing. */
class CastTransactionCoverageTest {
	private static final Path ROOT = Path.of(System.getProperty("user.dir"));

	@Test
	void everyMultiStageCastingRouteOwnsAnExplicitTransaction() throws IOException {
		String abilities = source("power/AbilityActivationService.java");
		String crystals = source("power/crystals/CrystalPowerRegistry.java");
		String spells = source("spell/SpellCastingManager.java");

		assertTrue(occurrences(abilities, "new CastTransaction()") >= 2,
				"ordinary casts and toggles must each be transactional");
		assertTrue(crystals.contains("new CastTransaction()"));
		assertTrue(spells.contains("new SpellCastTransaction"));
		assertTrue(spells.contains(".rollbackFull()"));
	}

	private static String source(String relative) throws IOException {
		return Files.readString(ROOT.resolve("src/main/java/com/powers").resolve(relative));
	}

	private static long occurrences(String source, String needle) {
		return source.lines().filter(line -> line.contains(needle)).count();
	}
}

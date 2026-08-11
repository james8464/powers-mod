package com.powers.spell;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prevents release notes from drifting back to retired ritual systems. */
class PracticalMagicDocumentationTest {
	@Test
	void readmeNamesTheCanonicalRitualsAndShadowDiagnostics() throws Exception {
		String readme = Files.readString(Path.of("README.md"));
		for (String spell : java.util.List.of("Soul Compass", "Augury", "Cartographer's Star",
				"Celestial Ruin", "Dimensional Anchor", "Blood Reading", "Grave Recall",
				"Purification Circle", "Verdant Tending", "Hearth Sanctuary",
				"Ward-Breaking Ritual", "Dispel")) {
			assertTrue(readme.contains(spell), spell);
		}
		assertTrue(readme.contains("latest 16 server-authoritative magic attempts"));
		assertTrue(readme.contains("override every player-consent gate"));
		assertTrue(readme.contains("hidden, inert compatibility aliases"));
		assertFalse(readme.contains("Those 23 innate actions, 21 grimoire spells"));
	}
}

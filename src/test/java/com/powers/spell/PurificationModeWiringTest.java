package com.powers.spell;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PurificationModeWiringTest {
	@Test
	void spellReadsButNeverConsumesTheOffhandCatalyst() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/powers/spell/SpellEffects.java"));
		assertTrue(source.contains("PurificationModeRules.fromCatalyst"));
		assertTrue(source.contains("InteractionHand.OFF_HAND"));
		assertFalse(source.contains("offhand.shrink"));
		assertFalse(source.contains("offhand.consume"));
	}

	@Test
	void purificationServiceOwnsAllThreeExclusiveActions() throws Exception {
		String source = Files.readString(Path.of(
				"src/main/java/com/powers/spell/PurificationService.java"));
		assertTrue(source.contains("case CLEANSE"));
		assertTrue(source.contains("case LINK_SEVER"));
		assertTrue(source.contains("case CORRUPTION_RELIEF"));
		assertTrue(source.contains("PowersEffects.AMETHYST_POISONING"));
	}
}

package com.powers.item;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RuneItemContractTest {
	@Test
	void runestonesDeclareSixtyFourStackCapacity() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/powers/item/RuneItem.java"));
		assertTrue(source.contains("super(properties.stacksTo(64))"));
	}

	@Test
	void useHasNoCooldownAndConsumesOnlyInsideSuccessfulRestore() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/powers/item/RuneItem.java"));
		assertFalse(source.contains("getCooldowns"));
		assertFalse(source.contains("cooldownTicks"));
		int restored = source.indexOf("if (data.regenerateEnergy(energy))");
		int consumed = source.indexOf("stack.shrink(1)");
		int failure = source.indexOf("rune.powers.full");
		assertTrue(restored >= 0 && consumed > restored && failure > consumed);
	}
}

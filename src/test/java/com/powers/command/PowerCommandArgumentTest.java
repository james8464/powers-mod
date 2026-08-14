package com.powers.command;

import com.mojang.brigadier.StringReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PowerCommandArgumentTest {
	@Test
	void travelDimensionConsumesACompleteNamespacedIdentifier() throws Exception {
		StringReader reader = new StringReader("minecraft:the_nether");

		assertEquals("minecraft:the_nether", PowerCommand.dimensionArgument().parse(reader));
		assertEquals(reader.getTotalLength(), reader.getCursor());
	}
}

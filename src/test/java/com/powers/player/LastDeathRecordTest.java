package com.powers.player;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LastDeathRecordTest {
	@Test
	void codecPreservesDimensionAndCoordinates() {
		LastDeathRecord original = new LastDeathRecord("minecraft:the_nether", -13, 64, 209);
		var encoded = LastDeathRecord.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
		assertEquals(original, LastDeathRecord.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
	}

	@Test
	void captureUsesMinecraftFlooringForNegativeCoordinates() {
		assertEquals(new LastDeathRecord("minecraft:overworld", -2, 70, 4),
				LastDeathRecord.at("minecraft:overworld", -1.01, 70.99, 4.0));
	}

	@Test
	void blankDimensionsAreRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> new LastDeathRecord(" ", 0, 0, 0));
	}
}

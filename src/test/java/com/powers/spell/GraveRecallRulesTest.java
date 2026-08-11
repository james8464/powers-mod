package com.powers.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import com.powers.player.LastDeathRecord;

class GraveRecallRulesTest {
	@Test void expiresUnknownLegacyAndOldMarkers() {
		assertTrue(new LastDeathRecord("minecraft:overworld", 0, 0, 0, 0).retained(10));
		assertTrue(new LastDeathRecord("minecraft:overworld", 0, 0, 0, 100).retained(100 + LastDeathRecord.RETENTION_TICKS));
		assertFalse(new LastDeathRecord("minecraft:overworld", 0, 0, 0, 100).retained(101 + LastDeathRecord.RETENTION_TICKS));
	}

	@Test void bearingExistsOnlyInTheRecordedDimension() {
		var death = new LastDeathRecord("minecraft:overworld", 20, 64, 0);
		assertEquals("spell.powers.grave_recall.direction.east", death.bearing("minecraft:overworld", 0, 0).orElseThrow());
		assertTrue(death.bearing("minecraft:the_nether", 0, 0).isEmpty());
	}
}

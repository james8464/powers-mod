package com.powers.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GraveRecallRulesTest {
	@Test void expiresUnknownLegacyAndOldMarkers() {
		assertFalse(GraveRecallRules.retained(0, 10));
		assertTrue(GraveRecallRules.retained(100, 100 + GraveRecallRules.RETENTION_TICKS));
		assertFalse(GraveRecallRules.retained(100, 101 + GraveRecallRules.RETENTION_TICKS));
	}

	@Test void bearingExistsOnlyInTheRecordedDimension() {
		assertEquals("east", GraveRecallRules.bearing("minecraft:overworld", 0, 0,
				"minecraft:overworld", 20, 0).orElseThrow());
		assertTrue(GraveRecallRules.bearing("minecraft:the_nether", 0, 0,
				"minecraft:overworld", 20, 0).isEmpty());
	}
}

package com.powers.power.artifact;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactFieldPulseRulesTest {
	@Test
	void fieldsPulseOncePerFourTicksWithStableOwnerStagger() {
		int pulses = 0;
		for (int tick = 0; tick < 40; tick++) {
			if (ArtifactFieldPulseRules.shouldPulse(tick, 7)) pulses++;
		}
		assertEquals(10, pulses);
		assertTrue(ArtifactFieldPulseRules.shouldPulse(3, 7));
		assertFalse(ArtifactFieldPulseRules.shouldPulse(4, 7));
	}
}

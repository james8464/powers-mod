package com.powers.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellFieldTimingTest {
	@Test
	void pulsesUseOneMonotonicServerTickClock() {
		assertEquals(125L, SpellFieldTiming.nextPulseAt(120L));
		assertFalse(SpellFieldTiming.ready(124L, 125L));
		assertTrue(SpellFieldTiming.ready(125L, 125L));
	}

	@Test
	void globalTimeStopPausesWorldOwnedFields() {
		assertFalse(SpellFieldTiming.mayAdvance(true));
		assertTrue(SpellFieldTiming.mayAdvance(false));
	}
}

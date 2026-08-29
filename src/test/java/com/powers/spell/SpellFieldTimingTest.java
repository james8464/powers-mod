package com.powers.spell;

import com.powers.time.WorldTick;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellFieldTimingTest {
	@Test
	void pulsesUseTheWorldClockOnly() {
		WorldTick next = SpellFieldTiming.nextPulseAt(WorldTick.at(120L));
		assertEquals(WorldTick.at(125L), next);
		assertFalse(SpellFieldTiming.ready(WorldTick.at(124L), next));
		assertTrue(SpellFieldTiming.ready(WorldTick.at(125L), next));
	}

	@Test
	void allVanillaFreezesPauseWorldOwnedFields() {
		assertFalse(SpellFieldTiming.mayAdvance(true));
		assertTrue(SpellFieldTiming.mayAdvance(false));
	}
}

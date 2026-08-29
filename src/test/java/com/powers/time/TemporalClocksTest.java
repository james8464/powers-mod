package com.powers.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalClocksTest {
	@Test
	void clockValuesRejectNegativeTicksAndSaturateDeadlines() {
		assertThrows(IllegalArgumentException.class, () -> ControlTick.at(-1L));
		assertThrows(IllegalArgumentException.class, () -> WorldTick.at(-1L));
		assertEquals(Long.MAX_VALUE, ControlTick.at(Long.MAX_VALUE - 2L).plus(10L).value());
		assertEquals(Long.MAX_VALUE, WorldTick.at(Long.MAX_VALUE - 2L).plus(10L).value());
		assertThrows(IllegalArgumentException.class, () -> ControlTick.at(1L).plus(-1L));
		assertThrows(IllegalArgumentException.class, () -> WorldTick.at(1L).plus(-1L));
	}

	@Test
	void deadlineMathCannotCrossClockKinds() throws Exception {
		ControlTick acquired = ControlTick.at(40L);
		ControlTick deadline = acquired.plus(20L);
		assertEquals(20L, deadline.elapsedSince(acquired));
		assertEquals(5L, ControlTick.at(55L).remainingUntil(deadline));
		assertEquals(0L, ControlTick.at(61L).remainingUntil(deadline));

		WorldTick started = WorldTick.at(100L);
		assertEquals(8L, WorldTick.at(108L).elapsedSince(started));
		assertEquals(ControlTick.class,
				ControlTick.class.getMethod("elapsedSince", ControlTick.class).getParameterTypes()[0]);
		assertEquals(WorldTick.class,
				WorldTick.class.getMethod("elapsedSince", WorldTick.class).getParameterTypes()[0]);
	}

	@Test
	void worldClockAdvancesOnlyWhenVanillaIsNotFrozen() {
		assertTrue(TemporalClocks.worldAdvances(false));
		assertFalse(TemporalClocks.worldAdvances(true));
	}

	@Test
	void parkedWorldTickNeverRepeatsCadenceWork() {
		WorldTick parked = WorldTick.at(120L);
		assertTrue(TemporalClocks.worldPulse(false, parked, 20L));
		assertFalse(TemporalClocks.worldPulse(true, parked, 20L));
		assertFalse(TemporalClocks.worldPulse(false, WorldTick.at(121L), 20L));
		assertThrows(IllegalArgumentException.class,
				() -> TemporalClocks.worldPulse(false, parked, 0L));
	}
}

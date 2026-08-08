package com.powers.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeadlineMathTest {
	@Test
	void remainingTicksUsesPersistentGameTimeAndClampsExpiredDeadlines() {
		assertEquals(25, DeadlineMath.remainingTicks(1_025L, 1_000L));
		assertEquals(0, DeadlineMath.remainingTicks(1_000L, 1_000L));
		assertEquals(0, DeadlineMath.remainingTicks(900L, 1_000L));
		assertEquals(Integer.MAX_VALUE,
				DeadlineMath.remainingTicks(Long.MAX_VALUE, 0L));
	}
}

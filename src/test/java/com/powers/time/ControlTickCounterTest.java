package com.powers.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControlTickCounterTest {
	@Test
	void signedMinecraftCounterWrapsIntoOneMonotonicLongEpoch() {
		ControlTickCounter counter = new ControlTickCounter();

		assertEquals(2_147_483_647L, counter.observe(Integer.MAX_VALUE).value());
		assertEquals(2_147_483_648L, counter.observe(Integer.MIN_VALUE).value());
		assertEquals(4_294_967_295L, counter.observe(-1).value());
		assertEquals(4_294_967_296L, counter.observe(0).value());
	}

	@Test
	void unexpectedSmallBackwardSampleCannotReverseControlTime() {
		ControlTickCounter counter = new ControlTickCounter();
		assertEquals(500L, counter.observe(500).value());
		assertEquals(500L, counter.observe(499).value());
	}
}

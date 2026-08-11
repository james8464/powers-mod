package com.powers.cooldown;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CooldownPresentationTest {
	@Test
	void wholeSecondsCeilPositivePartialTicks() {
		assertEquals(0L, CooldownPresentation.wholeSeconds(0));
		assertEquals(1L, CooldownPresentation.wholeSeconds(1));
		assertEquals(1L, CooldownPresentation.wholeSeconds(20));
		assertEquals(2L, CooldownPresentation.wholeSeconds(21));
	}

	@Test
	void tenthsRetainPlayerVisibleFractionalPrecision() {
		assertEquals("0.0", CooldownPresentation.tenths(0));
		assertEquals("0.1", CooldownPresentation.tenths(1));
		assertEquals("1.0", CooldownPresentation.tenths(20));
		assertEquals("1.1", CooldownPresentation.tenths(21));
	}

	@Test
	void negativeValuesPresentAsReady() {
		assertEquals(0L, CooldownPresentation.wholeSeconds(-1));
		assertEquals("0.0", CooldownPresentation.tenths(-1));
	}
}

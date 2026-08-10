package com.powers.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FxColorMathTest {
	@Test
	void rainbowWrapsAndHandlesNegativeTicksWithoutInvalidColours() {
		assertEquals(0xFF0000, FxColorMath.rainbow(0, 1));
		assertEquals(0xFF0000, FxColorMath.rainbow(360, 1));
		assertEquals(0xFF0000, FxColorMath.rainbow(-360, 1));
		assertEquals(0xFFFF00, FxColorMath.rainbow(60, 1));
	}
}

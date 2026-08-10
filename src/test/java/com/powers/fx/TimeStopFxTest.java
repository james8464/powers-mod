package com.powers.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeStopFxTest {
	@Test
	void boundaryCueStaysReadableWithoutScalingPastItsParticleCap() {
		assertEquals(24, TimeStopFx.boundaryPoints(4.0));
		assertEquals(48, TimeStopFx.boundaryPoints(24.0));
		assertEquals(56, TimeStopFx.boundaryPoints(48.0));
		assertEquals(56, TimeStopFx.boundaryPoints(500.0));
	}
}

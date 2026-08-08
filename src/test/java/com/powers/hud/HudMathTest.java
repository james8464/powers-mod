package com.powers.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudMathTest {
	@Test
	void energyArcClampsAndFillsProportionally() {
		assertEquals(0, HudMath.filledSegments(-10, 100, 24));
		assertEquals(12, HudMath.filledSegments(50, 100, 24));
		assertEquals(24, HudMath.filledSegments(500, 100, 24));
		assertEquals(0, HudMath.filledSegments(50, 0, 24));
	}

	@Test
	void visualModeHasUnambiguousPriority() {
		assertEquals(HudEnergyMode.DAMPENED, HudMath.mode(50, true, true));
		assertEquals(HudEnergyMode.EMPTY, HudMath.mode(0, false, true));
		assertEquals(HudEnergyMode.DARKNESS, HudMath.mode(50, false, true));
		assertEquals(HudEnergyMode.NORMAL, HudMath.mode(50, false, false));
		assertEquals(HudEnergyMode.PROJECTION, HudMath.mode(50, false, false, true));
		assertEquals(HudEnergyMode.DAMPENED, HudMath.mode(50, true, false, true));
	}

	@Test
	void cooldownRuneCountRoundsUpAndClamps() {
		assertEquals(0, HudMath.cooldownSegments(0, 100, 8));
		assertEquals(4, HudMath.cooldownSegments(50, 100, 8));
		assertEquals(8, HudMath.cooldownSegments(500, 100, 8));
	}
}

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
	void tenSymbolEnergyBarUsesTwentyVanillaStyleHalfSteps() {
		assertEquals(0, HudMath.energyHalfUnits(-1, 100));
		assertEquals(1, HudMath.energyHalfUnits(1, 100));
		assertEquals(10, HudMath.energyHalfUnits(50, 100));
		assertEquals(19, HudMath.energyHalfUnits(95, 100));
		assertEquals(20, HudMath.energyHalfUnits(100, 100));
		assertEquals(0, HudMath.energyHalfUnits(50, 0));
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

	@Test
	void elementalRunesDistinguishPrimedInactiveAndPulseStates() {
		assertEquals(0xFFFF5A24, HudMath.elementalRuneColor(0, 0, 0));
		assertEquals(0xCCFF5A24, HudMath.elementalRuneColor(0, 0, 5));
		assertEquals(0x5582E9FF, HudMath.elementalRuneColor(0, 1, 0));
		assertEquals(0x55FFF59D, HudMath.elementalRuneColor(0, 2, 0));
	}

	@Test
	void elementalRuneMathNormalizesMalformedPhaseValues() {
		assertEquals(0xFF8C66FF, HudMath.elementalRuneColor(-1, 3, 0));
		assertEquals(0xFFFF5A24, HudMath.elementalRuneColor(4, 4, 0));
	}

	@Test
	void secondStepRunesAlternateCyanGoldAndExchangePulse() {
		assertEquals(0xFFD7F8FF, HudMath.secondStepRuneColor(0, 0));
		assertEquals(0xCCFFD166, HudMath.secondStepRuneColor(1, 0));
		assertEquals(0xCCD7F8FF, HudMath.secondStepRuneColor(0, 4));
		assertEquals(0xFFFFD166, HudMath.secondStepRuneColor(1, 4));
		assertEquals(0xFFD7F8FF, HudMath.secondStepRuneColor(-2, -4));
	}
}

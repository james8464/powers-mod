package com.powers.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowSwordPaletteTest {
	@Test
	void corruptionKeepsAnEchoOfEachPowerWithoutProducingBrightVisualNoise() {
		ShadowSwordPalette.Palette fire = ShadowSwordPalette.corrupt(0xFF4500);
		ShadowSwordPalette.Palette frost = ShadowSwordPalette.corrupt(0x81D4FA);

		assertNotEquals(fire, frost);
		assertTrue(brightest(fire.primary()) <= 127);
		assertTrue(brightest(fire.secondary()) <= 127);
		assertTrue(brightest(frost.primary()) <= 127);
	}

	private static int brightest(int rgb) {
		return Math.max((rgb >>> 16) & 0xFF, Math.max((rgb >>> 8) & 0xFF, rgb & 0xFF));
	}
}

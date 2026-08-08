package com.powers.hud;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure bounds checks for the two overlay groups. */
class HudLayoutTest {
	@ParameterizedTest
	@CsvSource({"320,240", "427,240", "854,480", "1920,1080"})
	void allHudElementsStayInsideTheScaledWindow(int width, int height) {
		HudLayout layout = HudLayout.forScreen(width, height);
		for (HudLayout.Rect rect : layout.elements()) {
			assertTrue(rect.x() >= 0 && rect.y() >= 0, rect.toString());
			assertTrue(rect.right() <= width && rect.bottom() <= height, rect.toString());
		}
	}
}

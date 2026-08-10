package com.powers.hud;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

	@Test
	void energyUsesTenVanillaScaleSymbolsAlignedAboveHunger() {
		HudLayout layout = HudLayout.forScreen(427, 240);

		assertTrue(layout.energy().x() == 427 / 2 + 10);
		assertTrue(layout.energy().y() == 240 - 49);
		assertTrue(layout.energy().width() == 81);
		assertTrue(layout.energy().height() == 9);
	}

	@Test
	void airAndVehicleRowsMoveEnergyUpWithoutChangingHungerAlignment() {
		HudLayout baseline = HudLayout.forScreen(427, 240, 0, 0);
		HudLayout air = HudLayout.forScreen(427, 240, 1, 0);
		HudLayout mount = HudLayout.forScreen(427, 240, 0, 2);
		HudLayout both = HudLayout.forScreen(427, 240, 1, 2);

		assertTrue(air.energy().y() == baseline.energy().y() - 10);
		assertTrue(mount.energy().y() == baseline.energy().y() - 20);
		assertTrue(both.energy().y() == baseline.energy().y() - 30);
		assertTrue(both.energy().x() == baseline.energy().x());
	}

	@Test
	void tenSymbolCoordinatesExactlyMatchTheVanillaHungerStride() {
		HudLayout.Rect energy = HudLayout.forScreen(427, 240).energy();
		assertTrue(HudLayout.energySymbolX(energy, 0) == energy.right() - 9);
		assertTrue(HudLayout.energySymbolX(energy, 9) == energy.x());
	}

	@Test
	void verticalPowerRailDoesNotCoverTheHotbarOnNormalScaledWindows() {
		HudLayout layout = HudLayout.forScreen(427, 240);
		HudLayout.Rect hotbar = HudLayout.vanillaHotbar(427, 240);

		for (HudLayout.Rect slot : layout.powerSlots()) {
			assertFalse(slot.intersects(hotbar), slot + " overlaps " + hotbar);
		}
	}
}

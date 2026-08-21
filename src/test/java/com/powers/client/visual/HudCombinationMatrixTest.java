package com.powers.client.visual;

import com.powers.hud.HudLayout;
import com.powers.hud.HudPlacement;
import com.powers.hud.HudVisibility;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exhaustive geometry proof behind the smaller human-reviewable HUD contact sheet. */
class HudCombinationMatrixTest {
	@Test
	void everySupportedHudCaseIsUniqueScaledAndVanillaAligned() {
		Set<String> caseKeys = new HashSet<>();
		Set<String> scaledDimensions = new HashSet<>();
		Set<String> scaledLayoutOutputs = new HashSet<>();
		Set<String> energyStates = new HashSet<>();
		for (VisualGoldenHarness.HudCase hudCase : VisualGoldenHarness.hudCases()) {
			assertTrue(caseKeys.add(hudCase.key()), "duplicate HUD case: " + hudCase.key());
			scaledDimensions.add(hudCase.logicalWidth() + "x" + hudCase.logicalHeight());
			energyStates.add(hudCase.energyRow() + ":" + hudCase.halfUnits());

			HudLayout layout = HudLayout.forScreen(hudCase.logicalWidth(), hudCase.logicalHeight(),
					hudCase.airRows(), hudCase.mountRows(), HudPlacement.defaults());
			if (hudCase.heartRows() == 1 && hudCase.mountRows() == 0 && hudCase.airRows() == 0
					&& !hudCase.armour() && !hudCase.spectator()
					&& hudCase.energyRow() == 0 && hudCase.halfUnits() == 0) {
				scaledLayoutOutputs.add(layout.elements().toString());
			}
			for (HudLayout.Rect rectangle : layout.elements()) {
				assertTrue(rectangle.x() >= 0 && rectangle.y() >= 0);
				assertTrue(rectangle.right() <= hudCase.logicalWidth()
						&& rectangle.bottom() <= hudCase.logicalHeight());
			}
			assertEquals(hudCase.logicalWidth() / 2 + 10, layout.energy().x());
			assertEquals(layout.energy().x(), HudLayout.energySymbolX(layout.energy(), 9));
			assertEquals(layout.energy().right() - 9,
					HudLayout.energySymbolX(layout.energy(), 0));
			assertEquals(!hudCase.spectator(),
					HudVisibility.energy(true, hudCase.spectator(), false));
			for (HudLayout.Rect slot : layout.powerSlots()) {
				assertFalse(slot.intersects(HudLayout.vanillaHotbar(
						hudCase.logicalWidth(), hudCase.logicalHeight())));
			}
		}
		assertEquals(53_760, caseKeys.size());
		assertEquals(Set.of("1280x960", "640x480", "427x320", "320x240"), scaledDimensions);
		assertEquals(4, scaledLayoutOutputs.size(), "each GUI scale must change rendered geometry");
		assertEquals(105, energyStates.size(), "five rows must cover all 21 half-unit values");
	}
}

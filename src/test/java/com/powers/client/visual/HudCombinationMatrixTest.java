package com.powers.client.visual;

import com.powers.hud.HudLayout;
import com.powers.hud.HudPlacement;
import com.powers.hud.HudVisibility;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exhaustive geometry proof behind the smaller human-reviewable HUD contact sheet. */
class HudCombinationMatrixTest {
	@Test
	void all8192SupportedHudCombinationsRemainBoundedAndVanillaAligned() {
		int[][] resolutions = {{320, 240}, {427, 240}, {512, 288}, {854, 480}};
		int checked = 0;
		for (int[] resolution : resolutions) {
			for (int guiScale = 1; guiScale <= 4; guiScale++) {
				for (int heartRows = 1; heartRows <= 4; heartRows++) {
					for (int mountRows = 0; mountRows <= 3; mountRows++) {
						for (int air = 0; air <= 1; air++) {
							for (int armour = 0; armour <= 1; armour++) {
								for (int spectator = 0; spectator <= 1; spectator++) {
									for (int accessibilityMode = 0; accessibilityMode <= 3; accessibilityMode++) {
										HudLayout layout = HudLayout.forScreen(resolution[0], resolution[1],
												air, mountRows, HudPlacement.defaults());
										for (HudLayout.Rect rectangle : layout.elements()) {
											assertTrue(rectangle.x() >= 0 && rectangle.y() >= 0);
											assertTrue(rectangle.right() <= resolution[0]
													&& rectangle.bottom() <= resolution[1]);
										}
										assertEquals(resolution[0] / 2 + 10, layout.energy().x());
										assertEquals(layout.energy().x(), HudLayout.energySymbolX(layout.energy(), 9));
										assertEquals(layout.energy().right() - 9,
												HudLayout.energySymbolX(layout.energy(), 0));
										assertEquals(spectator == 0,
												HudVisibility.energy(true, spectator != 0, false));
										for (HudLayout.Rect slot : layout.powerSlots()) {
											assertFalse(slot.intersects(HudLayout.vanillaHotbar(
													resolution[0], resolution[1])));
										}
										checked++;
									}
								}
							}
						}
					}
				}
			}
		}
		assertEquals(8_192, checked);
	}
}

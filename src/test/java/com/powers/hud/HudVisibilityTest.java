package com.powers.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudVisibilityTest {
	@Test
	void energyFollowsTheVanillaSurvivalBarVisibility() {
		assertTrue(HudVisibility.energy(true, false, false));
		assertFalse(HudVisibility.energy(false, false, false));
		assertFalse(HudVisibility.energy(true, true, false));
		assertFalse(HudVisibility.energy(true, false, true));
	}
}

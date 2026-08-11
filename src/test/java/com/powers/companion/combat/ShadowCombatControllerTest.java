package com.powers.companion.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowCombatControllerTest {
	@Test
	void controllerCadenceAndCapsDoNotScaleWithWorldSize() {
		assertTrue(ShadowCombatController.shouldPlan(20, 0));
		assertFalse(ShadowCombatController.shouldPlan(21, 0));
		assertTrue(ShadowCombatController.MAX_TARGET_CANDIDATES <= 64);
		assertTrue(ShadowCombatController.CREDIT_WINDOW_TICKS <= 100);
	}
}

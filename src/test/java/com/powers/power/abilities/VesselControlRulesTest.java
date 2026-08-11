package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VesselControlRulesTest {
	@Test
	void movementIsNormalizedAndBoundedAgainstForgedInputs() {
		var movement = VesselControlRules.movement(0.0F, 100.0F, 100.0F, true, false);
		double horizontal = Math.hypot(movement.x(), movement.z());
		assertTrue(horizontal <= VesselControlRules.MAX_HORIZONTAL_STEP + 1.0E-9);
		assertEquals(VesselControlRules.VERTICAL_STEP, movement.y());
	}

	@Test
	void hotbarAndAttackTargetsStayInsideServerAuthority() {
		assertEquals(0, VesselControlRules.hotbarSlot(-5));
		assertEquals(8, VesselControlRules.hotbarSlot(99));
		assertTrue(VesselControlRules.mayAttack(36.0, true, false));
		assertFalse(VesselControlRules.mayAttack(Math.nextUp(36.0), true, false));
		assertFalse(VesselControlRules.mayAttack(1.0, false, false));
		assertFalse(VesselControlRules.mayAttack(1.0, true, true));
	}
}

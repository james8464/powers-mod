package com.powers.companion;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivateCompanionRulesTest {
	@Test
	void eligibilityRequiresEveryShadowCondition() {
		assertTrue(PrivateCompanionRules.eligible(true, true, true, false, true));
		assertFalse(PrivateCompanionRules.eligible(true, true, true, false, false));
		assertFalse(PrivateCompanionRules.eligible(false, true, true, false, true));
		assertFalse(PrivateCompanionRules.eligible(true, false, true, false, true));
		assertFalse(PrivateCompanionRules.eligible(true, true, false, false, true));
		assertFalse(PrivateCompanionRules.eligible(true, true, true, true, true));
	}

	@Test
	void followPointStaysBehindAndTeleportsOnlyPastTwentyBlocks() {
		Vec3 point = PrivateCompanionRules.followPoint(new Vec3(10, 64, 10), new Vec3(0, 0, 1));
		assertEquals(new Vec3(10, 64, 6.5), point);
		assertFalse(PrivateCompanionRules.shouldTeleport(new Vec3(0, 0, 0), new Vec3(20, 0, 0)));
		assertTrue(PrivateCompanionRules.shouldTeleport(new Vec3(0, 0, 0), new Vec3(20.01, 0, 0)));
	}

	@Test
	void interactionAuthenticatesSessionDistanceAndViewRay() {
		assertTrue(PrivateCompanionRules.mayInteract(9, 9, 25, 0.9));
		assertFalse(PrivateCompanionRules.mayInteract(8, 9, 25, 0.9));
		assertFalse(PrivateCompanionRules.mayInteract(9, 9, 65, 0.9));
		assertFalse(PrivateCompanionRules.mayInteract(9, 9, 25, 0.4));
		UUID owner = UUID.randomUUID();
		assertEquals(java.util.Set.of(owner), PrivateCompanionRules.recipients(owner));
	}
}

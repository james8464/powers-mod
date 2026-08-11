package com.powers.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowCompanionRulesTest {
	@Test
	void followAndTeleportBandsDoNotFightEachOther() {
		assertFalse(ShadowCompanionRules.shouldFollow(3.0 * 3.0));
		assertTrue(ShadowCompanionRules.shouldFollow(5.0 * 5.0));
		assertFalse(ShadowCompanionRules.shouldTeleport(12.0 * 12.0));
		assertTrue(ShadowCompanionRules.shouldTeleport(12.01 * 12.01));
	}

	@Test
	void hiddenAndRevealedStatesHaveExplicitPhysicalContracts() {
		var hidden = ShadowCompanionRules.presentation(false);
		assertFalse(hidden.globallyVisible());
		assertFalse(hidden.collidable());
		assertFalse(hidden.externallyVulnerable());

		var revealed = ShadowCompanionRules.presentation(true);
		assertTrue(revealed.globallyVisible());
		assertTrue(revealed.collidable());
		assertTrue(revealed.externallyVulnerable());
		assertTrue(ShadowCompanionRules.recallEnergy() * 4 >= ShadowCompanionRules.MAX_ENERGY);
	}
}

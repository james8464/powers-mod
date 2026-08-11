package com.powers.mind;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MindBodyDamageRulesTest {
	@Test
	void detachedAvatarIsImmuneButItsPhysicalBodyRemainsVulnerable() {
		assertFalse(MindBodyDamageRules.avatarMayTakeDamage(true));
		assertTrue(MindBodyDamageRules.avatarMayTakeDamage(false));
		assertTrue(MindBodyDamageRules.proxyDamageIsFatal(20.0F, 20.0F));
		assertFalse(MindBodyDamageRules.proxyDamageIsFatal(19.99F, 20.0F));
	}

	@Test
	void invalidDamageCannotTriggerFatalCleanup() {
		assertFalse(MindBodyDamageRules.proxyDamageIsFatal(Float.NaN, 20.0F));
		assertFalse(MindBodyDamageRules.proxyDamageIsFatal(-1.0F, 20.0F));
	}
}

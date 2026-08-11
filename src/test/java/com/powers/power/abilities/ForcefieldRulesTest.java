package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForcefieldRulesTest {
	@Test
	void wardSharesWithinTwoBlocksAndNeverExpiresByTime() {
		assertTrue(ForcefieldRules.withinSharingRadius(0.0));
		assertTrue(ForcefieldRules.withinSharingRadius(4.0));
		assertFalse(ForcefieldRules.withinSharingRadius(4.0001));
		assertTrue(ForcefieldRules.expiryTick() > 1_000_000_000L);
	}

	@Test
	void onlyAdministrativeAndVoidDamageBypassTheWard() {
		assertTrue(ForcefieldRules.interceptable(false, false, 50_000.0F));
		assertTrue(ForcefieldRules.interceptable(false, false, Float.MAX_VALUE));
		assertFalse(ForcefieldRules.interceptable(true, false, 1.0F));
		assertFalse(ForcefieldRules.interceptable(false, true, 1.0F));
		assertFalse(ForcefieldRules.interceptable(false, false, 0.0F));
		assertFalse(ForcefieldRules.interceptable(false, false, Float.NaN));
	}
}

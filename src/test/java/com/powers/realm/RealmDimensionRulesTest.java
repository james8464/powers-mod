package com.powers.realm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmDimensionRulesTest {
	@Test
	void onlyPowersMindscapesForceRealmGameplayRules() {
		assertTrue(RealmDimensionRules.isMindscape("powers:dark_realm"));
		assertTrue(RealmDimensionRules.isMindscape("powers:light_realm"));
		assertTrue(RealmDimensionRules.isMindscape("powers:middleworld"));
		assertFalse(RealmDimensionRules.isMindscape("other_mod:dark_realm"));
		assertFalse(RealmDimensionRules.isMindscape("minecraft:overworld"));
		assertFalse(RealmDimensionRules.isMindscape(null));
	}
}

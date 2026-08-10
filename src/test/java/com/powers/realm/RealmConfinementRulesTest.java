package com.powers.realm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RealmConfinementRulesTest {
	@Test
	void deathCannotBypassDarkRealmDepartureRequirements() {
		assertEquals("powers:dark_realm", RealmConfinementRules.requiredRespawnRealm(
				"powers:dark_realm", false, 10, 10));
		assertEquals("powers:dark_realm", RealmConfinementRules.requiredRespawnRealm(
				"powers:dark_realm", true, 0, 4));
		assertNull(RealmConfinementRules.requiredRespawnRealm(
				"powers:dark_realm", true, 0, 5));
	}

	@Test
	void deathCannotBypassLightRealmDepartureRequirements() {
		assertEquals("powers:light_realm", RealmConfinementRules.requiredRespawnRealm(
				"powers:light_realm", false, 4, 4));
		assertNull(RealmConfinementRules.requiredRespawnRealm(
				"powers:light_realm", false, 5, 0));
		assertNull(RealmConfinementRules.requiredRespawnRealm(
				"powers:light_realm", true, 0, 5));
		assertNull(RealmConfinementRules.requiredRespawnRealm(
				"minecraft:overworld", false, 0, 0));
	}

	@Test
	void missingRequiredRealmFailsClosedIntoARecoveryHold() {
		assertEquals(RealmConfinementRules.Enforcement.ALLOW,
				RealmConfinementRules.enforcement(null, false));
		assertEquals(RealmConfinementRules.Enforcement.TRANSFER,
				RealmConfinementRules.enforcement("powers:dark_realm", true));
		assertEquals(RealmConfinementRules.Enforcement.LOCKED_HOLD,
				RealmConfinementRules.enforcement("powers:dark_realm", false));
	}
}

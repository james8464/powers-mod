package com.powers.realm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmPortalRulesTest {
	@Test
	void underQualifiedPlayersCannotUseVanillaPortalsToEscapeEitherRealm() {
		assertFalse(RealmPortalRules.mayDepart("powers:dark_realm", false, 10, 10));
		assertFalse(RealmPortalRules.mayDepart("powers:dark_realm", true, 10, 4));
		assertFalse(RealmPortalRules.mayDepart("powers:light_realm", false, 4, 4));
	}

	@Test
	void qualifiedAndOrdinaryWorldPlayersMayUseVanillaPortals() {
		assertTrue(RealmPortalRules.mayDepart("powers:dark_realm", true, 0, 5));
		assertTrue(RealmPortalRules.mayDepart("powers:light_realm", false, 5, 0));
		assertTrue(RealmPortalRules.mayDepart("powers:light_realm", true, 0, 5));
		assertTrue(RealmPortalRules.mayDepart("minecraft:overworld", false, 0, 0));
	}
}

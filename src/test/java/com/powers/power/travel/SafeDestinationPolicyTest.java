package com.powers.power.travel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SafeDestinationPolicyTest {
	@Test
	void maximumBuildHeightIsExclusive() {
		assertEquals(DestinationFailure.OUT_OF_BOUNDS,
				SafeDestinationResolver.boundsFailure(0, 320, 0, -64, 320,
						-1000, 1000, -1000, 1000));
	}

	@Test
	void rejectsNanAndInfinity() {
		assertEquals(DestinationFailure.OUT_OF_BOUNDS,
				SafeDestinationResolver.boundsFailure(Double.NaN, 64, 0, -64, 320,
						-1000, 1000, -1000, 1000));
		assertEquals(DestinationFailure.OUT_OF_BOUNDS,
				SafeDestinationResolver.boundsFailure(0, 64, Double.POSITIVE_INFINITY, -64, 320,
						-1000, 1000, -1000, 1000));
	}

	@Test
	void acceptsAFinitePointInsideBorderAndHeight() {
		assertEquals(DestinationFailure.NONE,
				SafeDestinationResolver.boundsFailure(10, 64, -10, -64, 320,
						-1000, 1000, -1000, 1000));
	}

	@Test
	void middleworldAcceptsItsCrystalButNotOrdinaryTeleportPowers() {
		assertEquals(DestinationFailure.REALM_RESTRICTED, SafeDestinationResolver.realmFailure(
				"minecraft:overworld", "powers:middleworld", TravelKind.POWER, false, 10, 0));
		assertEquals(DestinationFailure.NONE, SafeDestinationResolver.realmFailure(
				"minecraft:overworld", "powers:middleworld", TravelKind.CRYSTAL, false, 10, 0));
	}

	@Test
	void darkRealmDepartureRequiresReturningToTheBodyBeforeAnyOtherModTravel() {
		for (TravelKind kind : new TravelKind[] {
				TravelKind.POWER, TravelKind.CRYSTAL, TravelKind.PROJECTION, TravelKind.COMPANION}) {
			assertEquals(DestinationFailure.REALM_RESTRICTED, SafeDestinationResolver.realmFailure(
					"powers:dark_realm", "minecraft:overworld", kind, false, 10, 10));
			assertEquals(DestinationFailure.REALM_RESTRICTED, SafeDestinationResolver.realmFailure(
					"powers:dark_realm", "minecraft:overworld", kind, true, 10, 4));
			assertEquals(DestinationFailure.REALM_RESTRICTED, SafeDestinationResolver.realmFailure(
					"powers:dark_realm", "minecraft:overworld", kind, true, 0, 5));
		}
	}

	@Test
	void lightRealmDepartureStillRequiresAnExplicitBodyReturnAtLevelFive() {
		assertEquals(DestinationFailure.REALM_RESTRICTED, SafeDestinationResolver.realmFailure(
				"powers:light_realm", "minecraft:overworld", TravelKind.POWER, false, 4, 4));
		assertEquals(DestinationFailure.REALM_RESTRICTED, SafeDestinationResolver.realmFailure(
				"powers:light_realm", "minecraft:overworld", TravelKind.POWER, false, 5, 0));
		assertEquals(DestinationFailure.REALM_RESTRICTED, SafeDestinationResolver.realmFailure(
				"powers:light_realm", "minecraft:overworld", TravelKind.CRYSTAL, false, 0, 5));
	}

	@Test
	void playerReturnObeysRealmGatesButAdministrativeRecoveryCannotBeTrapped() {
		assertEquals(DestinationFailure.REALM_RESTRICTED, SafeDestinationResolver.realmFailure(
				"powers:dark_realm", "minecraft:overworld", TravelKind.PLAYER_RETURN,
				false, 0, 0));
		assertEquals(DestinationFailure.REALM_RESTRICTED, SafeDestinationResolver.realmFailure(
				"powers:light_realm", "minecraft:overworld", TravelKind.PLAYER_RETURN,
				false, 0, 0));
		assertEquals(DestinationFailure.NONE, SafeDestinationResolver.realmFailure(
				"powers:dark_realm", "minecraft:overworld", TravelKind.ADMIN_RECOVERY,
				false, 0, 0));
		assertEquals(DestinationFailure.NONE, SafeDestinationResolver.realmFailure(
				"powers:light_realm", "minecraft:overworld", TravelKind.ADMIN_RECOVERY,
				false, 0, 0));
	}

	@Test
	void darkRealmEntryStillAllowsItsCrystalButRejectsUngatedTeleportPowers() {
		assertEquals(DestinationFailure.REALM_RESTRICTED, SafeDestinationResolver.realmFailure(
				"minecraft:overworld", "powers:dark_realm", TravelKind.POWER, false, 10, 10));
		assertEquals(DestinationFailure.NONE, SafeDestinationResolver.realmFailure(
				"minecraft:overworld", "powers:dark_realm", TravelKind.CRYSTAL, false, 0, 0));
	}

	@Test
	void everyRouteMayMoveWithinTheSameMindscapeButOnlyRecoveryCanBypassDeparture() {
		for (TravelKind kind : TravelKind.values()) {
			assertEquals(DestinationFailure.NONE, SafeDestinationResolver.realmFailure(
					"powers:dark_realm", "powers:dark_realm", kind, false, 0, 0), kind.name());
			if (kind == TravelKind.ADMIN_RECOVERY || kind == TravelKind.FATAL_SOUL_RETURN) {
				assertEquals(DestinationFailure.NONE, SafeDestinationResolver.realmFailure(
						"powers:dark_realm", "minecraft:overworld", kind, false, 0, 0), kind.name());
			}
		}
	}

	@Test
	void qualifiedBodyReturnMayLeaveTheMindscape() {
		assertEquals(DestinationFailure.NONE, SafeDestinationResolver.realmFailure(
				"powers:dark_realm", "minecraft:overworld", TravelKind.PLAYER_RETURN,
				true, 0, 5));
		assertEquals(DestinationFailure.NONE, SafeDestinationResolver.realmFailure(
				"powers:light_realm", "minecraft:overworld", TravelKind.PLAYER_RETURN,
				false, 5, 0));
	}
}

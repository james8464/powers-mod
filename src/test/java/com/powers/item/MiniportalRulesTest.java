package com.powers.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiniportalRulesTest {
	@Test
	void newAndCorruptDevicesNormalizeToTheBoundedTwoChargeLifecycle() {
		assertEquals(2, MiniportalRules.charges(null));
		assertEquals(0, MiniportalRules.charges(-5));
		assertEquals(2, MiniportalRules.charges(99));
		assertEquals(1, MiniportalRules.afterSuccessfulTravel(2));
		assertEquals(0, MiniportalRules.afterSuccessfulTravel(1));
		assertEquals(0, MiniportalRules.afterSuccessfulTravel(0));
		assertEquals(2, MiniportalRules.afterRecharge());
	}

	@Test
	void travelRequiresChargeAndAnAnchorInTheCurrentDimension() {
		assertTrue(MiniportalRules.mayTravel(2, true));
		assertFalse(MiniportalRules.mayTravel(0, true));
		assertFalse(MiniportalRules.mayTravel(2, false));
	}

	@Test
	void delayedCommitRequiresTheOriginalOwnedDeviceAtTheReservedCharge() {
		assertTrue(MiniportalRules.mayCommit(true, true, true, true, 2, 2));
		assertFalse(MiniportalRules.mayCommit(false, true, true, true, 2, 2));
		assertFalse(MiniportalRules.mayCommit(true, false, true, true, 2, 2));
		assertFalse(MiniportalRules.mayCommit(true, true, false, true, 2, 2));
		assertFalse(MiniportalRules.mayCommit(true, true, true, false, 2, 2));
		assertFalse(MiniportalRules.mayCommit(true, true, true, true, 2, 1));
	}

	@Test
	void vanillaItemBarReflectsTheTwoChargeState() {
		assertEquals(13, MiniportalRules.barWidth(2));
		assertEquals(7, MiniportalRules.barWidth(1));
		assertEquals(0, MiniportalRules.barWidth(0));
	}

	@Test
	void namedAnchorPresentationIsBoundedAndNeverBlank() {
		assertEquals("Home", MiniportalRules.anchorName("  Home  ", "fallback"));
		assertEquals("fallback", MiniportalRules.anchorName("   ", "fallback"));
		assertEquals(48, MiniportalRules.anchorName("x".repeat(80), "fallback").length());
		assertTrue(MiniportalRules.chargedModel(1));
		assertFalse(MiniportalRules.chargedModel(0));
	}
}

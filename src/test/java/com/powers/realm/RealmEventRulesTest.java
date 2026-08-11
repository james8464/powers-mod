package com.powers.realm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RealmEventRulesTest {
	@Test
	void eachMindscapeHasATwoMinuteForceEventInATwelveMinuteCycle() {
		assertEquals(RealmEventType.NONE, RealmEventRules.eventAt(RealmKind.DARK, 0));
		assertEquals(RealmEventType.NONE, RealmEventRules.eventAt(RealmKind.LIGHT, 11_999));
		assertEquals(RealmEventType.DARK_ECLIPSE,
				RealmEventRules.eventAt(RealmKind.DARK, 12_000));
		assertEquals(RealmEventType.WHITEOUT,
				RealmEventRules.eventAt(RealmKind.LIGHT, 12_000));
		assertEquals(RealmEventType.NONE, RealmEventRules.eventAt(RealmKind.DARK, 14_400));
	}

	@Test
	void pressureRisesWithDistanceAndDuringAnEvent() {
		assertEquals(0, RealmEventRules.pressureTier(0, false));
		assertEquals(1, RealmEventRules.pressureTier(30, false));
		assertEquals(2, RealmEventRules.pressureTier(55, false));
		assertEquals(3, RealmEventRules.pressureTier(78, false));
		assertEquals(2, RealmEventRules.pressureTier(30, true));
	}
}

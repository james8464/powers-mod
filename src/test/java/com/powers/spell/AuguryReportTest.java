package com.powers.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuguryReportTest {
	@Test
	void weatherClassificationUsesTheStrongestObservedState() {
		assertEquals(AuguryReport.Weather.CLEAR, AuguryReport.weather(false, false));
		assertEquals(AuguryReport.Weather.RAIN, AuguryReport.weather(true, false));
		assertEquals(AuguryReport.Weather.THUNDER, AuguryReport.weather(true, true));
	}

	@Test
	void moonNamesCoverEveryVanillaPhase() {
		assertEquals("full", AuguryReport.moonName(0));
		assertEquals("new", AuguryReport.moonName(4));
		assertEquals("waxing_gibbous", AuguryReport.moonName(7));
		assertEquals("full", AuguryReport.moonName(8));
	}

	@Test
	void realmEventCountdownIsDeterministicAcrossCycleBoundaries() {
		assertEquals(12_000L, AuguryReport.ticksUntilRealmEvent(0));
		assertEquals(1L, AuguryReport.ticksUntilRealmEvent(11_999));
		assertEquals(0L, AuguryReport.ticksUntilRealmEvent(12_000));
		assertEquals(12_001L, AuguryReport.ticksUntilRealmEvent(14_399));
	}
}

package com.powers.spell;

import com.powers.force.LoadedFrontForecast;
import com.powers.realm.RealmEventType;
import com.powers.realm.RealmKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	void upcomingWhiteoutForecastUsesFutureActiveSeverity() {
		AuguryReport.RealmForecast event = AuguryReport.realmForecast(
				RealmKind.LIGHT, 11_000L, 55.0);

		assertEquals(RealmEventType.WHITEOUT, event.event());
		assertFalse(event.active());
		assertEquals(3, event.severityTier());
		assertEquals(1_000L, event.ticksUntilTransition());
	}

	@Test
	void eventFreeRealmExposesAProvisionalWindow() {
		AuguryReport.RealmForecast event = AuguryReport.realmForecast(
				RealmKind.LIGHT, 11_000L, 55.0);
		AuguryReport.RitualWindow window = AuguryReport.ritualWindow(event,
				LoadedFrontForecast.none(), LoadedFrontForecast.none());

		assertEquals(AuguryReport.RitualSafety.PROVISIONAL_WINDOW, window.safety());
		assertEquals(1_000L, window.ticksUntilTransition());
		assertEquals(AuguryReport.Uncertainty.LOADED_CHUNKS_ONLY, window.uncertainty());
	}

	@Test
	void activeDarkEclipseForecastReportsTimeUntilTheHazardEnds() {
		AuguryReport.RealmForecast event = AuguryReport.realmForecast(
				RealmKind.DARK, 12_400L, 10.0);
		AuguryReport.RitualWindow window = AuguryReport.ritualWindow(event,
				LoadedFrontForecast.none(), LoadedFrontForecast.none());

		assertEquals(RealmEventType.DARK_ECLIPSE, event.event());
		assertTrue(event.active());
		assertEquals(1, event.severityTier());
		assertEquals(2_000L, event.ticksUntilTransition());
		assertEquals(AuguryReport.RitualSafety.EVENT_HAZARD, window.safety());
		assertEquals(2_000L, window.ticksUntilTransition());
	}

	@Test
	void observedLoadedFrontMakesAnOtherwiseOpenWindowHazardous() {
		AuguryReport.RealmForecast event = AuguryReport.realmForecast(null, 4_000L, 0.0);
		LoadedFrontForecast darkness = new LoadedFrontForecast(3, 7, false);
		AuguryReport.RitualWindow window = AuguryReport.ritualWindow(event, darkness,
				LoadedFrontForecast.none());

		assertEquals(AuguryReport.RitualSafety.LOADED_FRONT_HAZARD, window.safety());
		assertEquals(-1L, window.ticksUntilTransition());
		assertEquals(AuguryReport.Uncertainty.LOADED_CHUNKS_ONLY, window.uncertainty());
	}

	@Test
	void exhaustedEmptyFrontSampleMakesTheRitualWindowIndeterminate() {
		AuguryReport.RealmForecast event = AuguryReport.realmForecast(null, 4_000L, 0.0);
		LoadedFrontForecast incomplete = new LoadedFrontForecast(0, -1, true);

		AuguryReport.RitualWindow window = AuguryReport.ritualWindow(event, incomplete,
				LoadedFrontForecast.none());

		assertEquals(AuguryReport.RitualSafety.INDETERMINATE_SAMPLE, window.safety());
		assertEquals(-1L, window.ticksUntilTransition());
	}
}

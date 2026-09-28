package com.powers.realm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiddleworldOutpostRulesTest {
	@Test
	void outpostsAndRoadsStayInsideTheSmallLoadedWorldBudget() {
		assertEquals(3, MiddleworldOutpostRules.outposts().size());
		assertTrue(MiddleworldOutpostRules.maxBlocksPerPulse() <= 64);
		assertTrue(MiddleworldOutpostRules.roadLength() <= 80);
		assertFalse(MiddleworldOutpostRules.createsTeleportHub());
	}

	@Test
	void dreamWeatherCyclesWithoutUsingRealPrecipitation() {
		assertEquals(MiddleworldOutpostRules.DreamWeather.STILL,
				MiddleworldOutpostRules.weatherAt(0));
		assertEquals(MiddleworldOutpostRules.DreamWeather.MOTES,
				MiddleworldOutpostRules.weatherAt(MiddleworldOutpostRules.WEATHER_PERIOD_TICKS));
		assertEquals(MiddleworldOutpostRules.DreamWeather.ECHOES,
				MiddleworldOutpostRules.weatherAt(MiddleworldOutpostRules.WEATHER_PERIOD_TICKS * 2L));
		assertFalse(MiddleworldOutpostRules.usesPrecipitation());
	}
}

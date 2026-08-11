package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeFreezeDrainRulesTest {
	@Test
	void globalClockControlBurnsThroughAnyFullPoolInAboutSevenSeconds() {
		assertEquals(40, TimeFreezeDrainRules.energyPerSecond(250));
		assertEquals(116, TimeFreezeDrainRules.energyPerSecond(770));
		assertTrue(TimeFreezeDrainRules.energyPerSecond(1_850) >= 278);
	}

	@Test
	void forecastUsesTheAuthoritativeDrainAndOnlyCountsPayableSeconds() {
		TimeFreezeDrainRules.Forecast forecast = TimeFreezeDrainRules.forecast(250, 250, 40.0);

		assertEquals(40, forecast.energyPerSecond());
		assertEquals(6, forecast.safeSeconds());
		assertFalse(forecast.lowTpsWarning());
		assertEquals(0, TimeFreezeDrainRules.forecast(39, 250, 40.0).safeSeconds());
	}

	@Test
	void highMsptWarnsWithoutRemovingTheForecast() {
		assertFalse(TimeFreezeDrainRules.forecast(250, 250, 50.0).lowTpsWarning());
		TimeFreezeDrainRules.Forecast warning = TimeFreezeDrainRules.forecast(250, 250, 50.01);

		assertTrue(warning.lowTpsWarning());
		assertEquals(40, warning.energyPerSecond());
		assertEquals(6, warning.safeSeconds());
	}
}

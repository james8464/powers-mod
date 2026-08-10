package com.powers.power.abilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeFreezeDrainRulesTest {
	@Test
	void globalClockControlBurnsThroughAnyFullPoolInAboutSevenSeconds() {
		assertEquals(40, TimeFreezeDrainRules.energyPerSecond(250));
		assertEquals(116, TimeFreezeDrainRules.energyPerSecond(770));
		assertTrue(TimeFreezeDrainRules.energyPerSecond(1_850) >= 278);
	}
}

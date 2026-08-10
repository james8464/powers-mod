package com.powers.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CelestialRuinRulesTest {
	@Test
	void ritualCountsDownForExactlyOneMinute() {
		assertEquals(1_200, CelestialRuinRules.COUNTDOWN_TICKS);
		assertEquals(50, CelestialRuinRules.BEAM_RADIUS);
	}

	@Test
	void detonationIsAtLeastTwentyTimesTheLivingForcePeakDamage() {
		assertTrue(CelestialRuinRules.PEAK_DAMAGE >= 2_000.0f);
	}

	@Test
	void destructionSphereHasAHardBoundary() {
		assertTrue(CelestialRuinRules.insideBlast(120, 0, 0));
		assertFalse(CelestialRuinRules.insideBlast(121, 0, 0));
	}

	@Test
	void livingForceCleanupSurvivesOptionalTerrainSafetyPolicy() {
		assertTrue(CelestialRuinRules.shouldDestroy(true, true, false, false));
		assertTrue(CelestialRuinRules.shouldDestroy(true, false, false, false));
		assertFalse(CelestialRuinRules.shouldDestroy(false, false, false, true));
		assertFalse(CelestialRuinRules.shouldDestroy(false, true, true, false));
		assertTrue(CelestialRuinRules.shouldDestroy(false, true, true, true));
	}
}

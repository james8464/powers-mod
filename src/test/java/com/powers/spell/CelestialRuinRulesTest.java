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
	void chunkTicketsAreDeferredAndGrowOnlyDuringTheFinalFiveSeconds() {
		assertEquals(-1, CelestialRuinTicketRules.radiusForCountdown(1_200, false, 9));
		assertEquals(-1, CelestialRuinTicketRules.radiusForCountdown(101, false, 9));
		assertEquals(1, CelestialRuinTicketRules.radiusForCountdown(100, false, 9));
		assertEquals(5, CelestialRuinTicketRules.radiusForCountdown(50, false, 9));
		assertEquals(9, CelestialRuinTicketRules.radiusForCountdown(0, false, 9));
		assertEquals(9, CelestialRuinTicketRules.radiusForCountdown(900, true, 9));
	}

	@Test
	void detonationIsAtLeastTwentyTimesTheLivingForcePeakDamage() {
		assertTrue(CelestialRuinRules.PEAK_DAMAGE >= 20_000.0f);
		assertTrue(CelestialRuinRules.DAMAGE_RADIUS >= 2_048);
		assertTrue(CelestialRuinRules.damage(0.0) >= 20_000.0f);
		assertTrue(CelestialRuinRules.damage(1_000.0) >= 100.0f);
		assertTrue(CelestialRuinRules.damage(2_047.0) > 0.0f);
		assertEquals(0.0f, CelestialRuinRules.damage(2_048.0), 0.001f);
	}

	@Test
	void destructionSphereHasAHardBoundary() {
		assertTrue(CelestialRuinRules.insideBlast(120, 0, 0));
		assertFalse(CelestialRuinRules.insideBlast(121, 0, 0));
	}

	@Test
	void radialAftershockCreatesThousandsOfBlocksOfBoundedStreaks() {
		assertTrue(CelestialRuinRules.aftershockTotalSteps() >= 40_000);
		CelestialRuinRules.AftershockOffset first = CelestialRuinRules.aftershockOffset(0);
		CelestialRuinRules.AftershockOffset last = CelestialRuinRules.aftershockOffset(
				CelestialRuinRules.aftershockTotalSteps() - 1);
		assertTrue(Math.hypot(first.x(), first.z()) <= CelestialRuinRules.DAMAGE_RADIUS);
		assertTrue(Math.hypot(last.x(), last.z()) <= CelestialRuinRules.DAMAGE_RADIUS + 1.0);
		assertEquals(first, CelestialRuinRules.aftershockOffset(0));
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

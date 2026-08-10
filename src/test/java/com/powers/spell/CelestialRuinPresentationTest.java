package com.powers.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CelestialRuinPresentationTest {
	@Test
	void detonationFlashHoldsThenFadesAcrossThreeSeconds() {
		assertEquals(60, CelestialRuinPresentation.FLASH_TICKS);
		assertEquals(255, CelestialRuinPresentation.flashAlpha(60));
		assertEquals(255, CelestialRuinPresentation.flashAlpha(41));
		assertTrue(CelestialRuinPresentation.flashAlpha(20) < 255);
		assertEquals(0, CelestialRuinPresentation.flashAlpha(0));
	}

	@Test
	void beamPacketsRefreshBeforeTheirClientLeaseExpires() {
		assertTrue(CelestialRuinPresentation.BEAM_LEASE_TICKS
				> CelestialRuinPresentation.BEAM_REFRESH_TICKS);
	}
}

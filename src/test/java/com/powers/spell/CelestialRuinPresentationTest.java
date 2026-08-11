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
		assertTrue(CelestialRuinPresentation.BEAM_VIEW_RADIUS >= 1_024);
		assertTrue(CelestialRuinPresentation.clientBeamParticleCount() >= 64);
		assertTrue(CelestialRuinPresentation.clientBeamParticleCount() <= 128);
	}

	@Test
	void tinnitusOutlastsTheOpaqueFlashAndFadesCleanly() {
		assertTrue(CelestialRuinPresentation.RINGING_TICKS
				> CelestialRuinPresentation.FLASH_TICKS);
		assertEquals(1.0F, CelestialRuinPresentation.ringingVolume(
				CelestialRuinPresentation.RINGING_TICKS), 0.001F);
		assertTrue(CelestialRuinPresentation.ringingVolume(20) < 0.5F);
		assertEquals(0.0F, CelestialRuinPresentation.ringingVolume(0), 0.001F);
	}
}

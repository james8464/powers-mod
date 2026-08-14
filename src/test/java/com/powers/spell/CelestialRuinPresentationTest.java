package com.powers.spell;

import com.powers.fx.FxLodTier;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CelestialRuinPresentationTest {
	@Test
	void distantColumnUsesTheAlwaysVisibleParticlePath() throws Exception {
		String source = Files.readString(Path.of(System.getProperty("user.dir"),
				"src/client/java/com/powers/client/fx/ClientCelestialRuinFx.java"));
		assertFalse(source.contains("client.level.addParticle("),
				"ordinary client particles are culled beyond 32 blocks");
		assertEquals(3, source.split("addAlwaysVisibleParticle\\(", -1).length - 1);
		assertEquals(3, source.split("true,", -1).length - 1,
				"always-visible particles must also override vanilla's 32-block limiter");
	}

	@Test
	void detonationFlashHoldsThenFadesAcrossTwentySeconds() {
		assertEquals(400, CelestialRuinPresentation.FLASH_TICKS);
		assertEquals(255, CelestialRuinPresentation.flashAlpha(400));
		assertEquals(255, CelestialRuinPresentation.flashAlpha(341));
		assertTrue(CelestialRuinPresentation.flashAlpha(200) < 255);
		assertEquals(0, CelestialRuinPresentation.flashAlpha(0));
	}

	@Test
	void beamPacketsRefreshBeforeTheirClientLeaseExpires() {
		assertTrue(CelestialRuinPresentation.BEAM_LEASE_TICKS
				> CelestialRuinPresentation.BEAM_REFRESH_TICKS);
		assertTrue(CelestialRuinPresentation.BEAM_VIEW_RADIUS >= 6_000);
		assertTrue(CelestialRuinPresentation.BEAM_VERTICAL_SLICES >= 10,
				"a four-band ring stack is not a sky-height column");
		assertTrue(CelestialRuinPresentation.clientBeamParticleCount() >= 64);
		assertTrue(CelestialRuinPresentation.clientBeamParticleCount() <= 128);
	}

	@Test
	void distanceTiersRetainTheFullColumnBoundaryWhileReducingDensity() {
		var near = CelestialRuinPresentation.columnDensity(FxLodTier.NEAR);
		var mid = CelestialRuinPresentation.columnDensity(FxLodTier.MID);
		var far = CelestialRuinPresentation.columnDensity(FxLodTier.FAR);
		assertEquals(124, near.particleCount());
		assertEquals(50, mid.particleCount());
		assertEquals(25, far.particleCount());
		assertEquals(24, near.boundaryParticles());
		assertEquals(16, mid.boundaryParticles());
		assertEquals(12, far.boundaryParticles());
		assertTrue(far.verticalSlices() >= 6,
				"far observers still need a sky-height event silhouette");
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

	@Test
	void detonationAudioUsesARestrainedObserverTierGain() {
		assertEquals(1.0F, CelestialRuinPresentation.audioGain(FxLodTier.NEAR));
		assertEquals(0.55F, CelestialRuinPresentation.audioGain(FxLodTier.MID));
		assertEquals(0.28F, CelestialRuinPresentation.audioGain(FxLodTier.FAR));
		assertEquals(0.0F, CelestialRuinPresentation.audioGain(FxLodTier.HIDDEN));
	}
}

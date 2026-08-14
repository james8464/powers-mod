package com.powers.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Distance tiers must reduce density without changing an event's authored silhouette. */
class FxLodPolicyTest {
	@Test
	void localGeometryKeepsFullNearDensityAndBoundedMidSilhouette() {
		assertEquals(new FxLodPolicy.Decision(FxLodTier.NEAR, 48, true, true),
				FxLodPolicy.decide(32.0, 48, FxLodScope.LOCAL, FxShapeFamily.RUNE));
		var mid = FxLodPolicy.decide(144.0, 48, FxLodScope.LOCAL, FxShapeFamily.RUNE);
		assertEquals(FxLodTier.MID, mid.tier());
		assertEquals(24, mid.particleCount());
		assertTrue(mid.preserveSilhouette());
		assertTrue(mid.playSignatureAudio());
		assertEquals(FxLodTier.HIDDEN,
				FxLodPolicy.decide(257.0, 48, FxLodScope.LOCAL, FxShapeFamily.RUNE).tier());
	}

	@Test
	void eventScaleFarTierRetainsMinimumReadableGeometryAndAudio() {
		var beam = FxLodPolicy.decide(1_800.0, 64, FxLodScope.EVENT_SCALE, FxShapeFamily.BEAM);
		var rune = FxLodPolicy.decide(1_800.0, 64, FxLodScope.EVENT_SCALE, FxShapeFamily.RUNE);
		assertEquals(FxLodTier.FAR, beam.tier());
		assertEquals(8, beam.particleCount());
		assertEquals(15, rune.particleCount());
		assertTrue(beam.preserveSilhouette());
		assertTrue(rune.playSignatureAudio());
		assertFalse(FxLodPolicy.decide(2_049.0, 64,
				FxLodScope.EVENT_SCALE, FxShapeFamily.RING).visible());
	}

	@Test
	void celestialRangePreservesCatastrophicSilhouetteToSixThousandBlocks() {
		var far = FxLodPolicy.decide(5_999.0, 640,
				FxLodScope.CATASTROPHIC, FxShapeFamily.COLUMN);
		assertEquals(FxLodTier.FAR, far.tier());
		assertEquals(32, far.particleCount());
		assertTrue(far.preserveSilhouette());
		assertTrue(far.playSignatureAudio());
		assertFalse(FxLodPolicy.decide(6_001.0, 640,
				FxLodScope.CATASTROPHIC, FxShapeFamily.COLUMN).visible());
	}

	@Test
	void invalidDistanceAndEmptyRequestsFailClosed() {
		assertEquals(FxLodTier.HIDDEN,
				FxLodPolicy.decide(Double.NaN, 32, FxLodScope.LOCAL, FxShapeFamily.BEAM).tier());
		assertEquals(0,
				FxLodPolicy.decide(1.0, 0, FxLodScope.LOCAL, FxShapeFamily.BEAM).particleCount());
	}
}

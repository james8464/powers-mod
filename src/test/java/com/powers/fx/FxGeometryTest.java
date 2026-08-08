package com.powers.fx;

import com.powers.magic.fx.FxMotif;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exhaustive safety and accessibility tests for deterministic client geometry. */
class FxGeometryTest {
	@Test
	void everyMotifProducesFiniteBoundedGeometry() {
		for (FxMotif motif : FxMotif.values()) {
			List<FxGeometry.Point> points = FxGeometry.points(motif, 81, 5, 96);
			assertTrue(points.size() <= 96, motif.name());
			assertTrue(points.stream().allMatch(FxGeometry.Point::finite), motif.name());
		}
	}

	@Test
	void reducedMotionReplacesMovingGeometryWithStaticSigils() {
		assertEquals(FxMotif.RING, FxGeometry.accessibleMotif(FxMotif.SPIRAL, true));
		assertEquals(FxMotif.GLYPH, FxGeometry.accessibleMotif(FxMotif.FRACTURE, true));
	}

	@Test
	void distanceAndIntensityRespectTheClientBudget() {
		assertTrue(FxGeometry.budget(90.0, 1, 1.0) <= 12);
		assertEquals(0, FxGeometry.budget(200.0, 5, 1.0));
		assertTrue(FxGeometry.budget(4.0, 5, 1.0) <= 96);
	}
}

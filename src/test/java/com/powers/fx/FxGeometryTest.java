package com.powers.fx;

import com.powers.magic.fx.FxMotif;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
		assertEquals(14, FxGeometry.budget(90.0, 1, 1.0));
		assertEquals(12, FxGeometry.budget(200.0, 5, 1.0));
		assertEquals(0, FxGeometry.budget(257.0, 5, 1.0));
		assertTrue(FxGeometry.budget(4.0, 5, 1.0) <= 96);
	}

	@Test
	void frameScaleChangesPhysicalGeometryRatherThanOnlyParticleCount() {
		FxGeometry.Point point = new FxGeometry.Point(1.0, -2.0, 3.0);

		assertEquals(new FxGeometry.Point(2.0, -4.0, 6.0), FxGeometry.scale(point, 2.0));
	}

	@Test
	void invalidGeometryScaleCannotReachTheRenderer() {
		FxGeometry.Point point = new FxGeometry.Point(1.0, 2.0, 3.0);

		assertThrows(IllegalArgumentException.class, () -> FxGeometry.scale(point, -1.0));
		assertThrows(IllegalArgumentException.class,
				() -> FxGeometry.scale(point, Double.POSITIVE_INFINITY));
	}

	@Test
	void immutableGeometryIsReusedForIdenticalSemanticRequests() {
		List<FxGeometry.Point> first = FxGeometry.points(FxMotif.GLYPH, 12, 4, 48);
		List<FxGeometry.Point> second = FxGeometry.points(FxMotif.GLYPH, 12, 4, 48);
		assertTrue(first == second);
		assertTrue(FxGeometry.poolSize() <= FxGeometry.MAX_POOLED_GEOMETRIES);
	}

	@Test
	void reusableTransformBufferMatchesLegacyGeometryWithoutPerPointResults() {
		FxGeometry.Point source = new FxGeometry.Point(1.25, -0.5, 2.75);
		double scale = 1.35;
		double angle = 0.72;
		FxGeometry.Point expected = FxGeometry.transform(FxGeometry.scale(source, scale),
				com.powers.magic.fx.FxOrientation.BILLBOARD, angle);
		FxGeometry.TransformBuffer buffer = new FxGeometry.TransformBuffer();

		assertTrue(buffer == buffer.configure(scale,
				com.powers.magic.fx.FxOrientation.BILLBOARD, angle));
		assertTrue(buffer == buffer.apply(source));
		assertEquals(expected.x(), buffer.x(), 1.0E-12);
		assertEquals(expected.y(), buffer.y(), 1.0E-12);
		assertEquals(expected.z(), buffer.z(), 1.0E-12);
	}
}

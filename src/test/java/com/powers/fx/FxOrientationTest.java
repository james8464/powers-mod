package com.powers.fx;

import com.powers.magic.fx.FxMotif;
import com.powers.magic.fx.FxOrientation;
import org.junit.jupiter.api.Test;

import static com.powers.magic.fx.FxOrientation.AUTO;
import static com.powers.magic.fx.FxOrientation.BILLBOARD;
import static com.powers.magic.fx.FxOrientation.GROUND;
import static com.powers.magic.fx.FxOrientation.NATIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifies the pure local-space transforms used to stage magic for each observer. */
class FxOrientationTest {
	@Test
	void groundOrientationLaysTheVerticalPlaneAcrossXZ() {
		FxGeometry.Point transformed = FxGeometry.transform(
				new FxGeometry.Point(1.0, 2.0, 3.0), GROUND, 0.0);

		assertEquals(new FxGeometry.Point(1.0, 3.0, 2.0), transformed);
	}

	@Test
	void billboardOrientationRotatesAroundWorldY() {
		FxGeometry.Point transformed = FxGeometry.transform(
				new FxGeometry.Point(1.0, 2.0, 3.0), BILLBOARD, Math.PI / 2.0);

		assertPoint(-3.0, 2.0, 1.0, transformed);
	}

	@Test
	void rotationsPreserveDistanceFromTheEffectOrigin() {
		FxGeometry.Point point = new FxGeometry.Point(2.0, -3.0, 4.0);
		double expectedSquared = 29.0;

		assertEquals(expectedSquared, squared(FxGeometry.transform(point, NATIVE, 0.7)), 1.0E-9);
		assertEquals(expectedSquared, squared(FxGeometry.transform(point, GROUND, 0.7)), 1.0E-9);
		assertEquals(expectedSquared, squared(FxGeometry.transform(point, BILLBOARD, 0.7)), 1.0E-9);
	}

	@Test
	void automaticOrientationBillboardsOnlyVerticalSignatureFamilies() {
		assertEquals(BILLBOARD, AUTO.resolve(FxMotif.GLYPH));
		assertEquals(BILLBOARD, AUTO.resolve(FxMotif.ECLIPSE));
		assertEquals(BILLBOARD, AUTO.resolve(FxMotif.FORK));
		assertEquals(NATIVE, AUTO.resolve(FxMotif.RING));
		assertEquals(GROUND, GROUND.resolve(FxMotif.GLYPH));
	}

	@Test
	void invalidAnglesCannotReachParticleCoordinates() {
		FxGeometry.Point point = new FxGeometry.Point(1.0, 2.0, 3.0);

		assertThrows(IllegalArgumentException.class,
				() -> FxGeometry.transform(point, BILLBOARD, Double.NaN));
		assertThrows(IllegalArgumentException.class,
				() -> FxGeometry.transform(point, AUTO, 0.0));
	}

	private static double squared(FxGeometry.Point point) {
		return point.x() * point.x() + point.y() * point.y() + point.z() * point.z();
	}

	private static void assertPoint(double x, double y, double z, FxGeometry.Point actual) {
		assertEquals(x, actual.x(), 1.0E-9);
		assertEquals(y, actual.y(), 1.0E-9);
		assertEquals(z, actual.z(), 1.0E-9);
	}
}

package com.powers.visual;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightRealmSkyGeometryTest {
	@Test
	void everyTriangleFanFacesDownTowardThePlayer() {
		for (LightRealmSkyProfile.Shape shape : LightRealmSkyProfile.Shape.values()) {
			LightRealmSkyGeometry.Mesh mesh = LightRealmSkyGeometry.build(shape);
			for (LightRealmSkyGeometry.DrawRange range : mesh.drawRanges()) {
				for (int offset = 1; offset < range.vertexCount() - 1; offset++) {
					LightRealmSkyGeometry.Vertex a = mesh.vertices().get(range.firstVertex());
					LightRealmSkyGeometry.Vertex b = mesh.vertices().get(range.firstVertex() + offset);
					LightRealmSkyGeometry.Vertex c = mesh.vertices().get(range.firstVertex() + offset + 1);
					double normalY = (b.z() - a.z()) * (c.x() - a.x())
							- (b.x() - a.x()) * (c.z() - a.z());
					assertTrue(normalY < -1.0E-5, shape + " contains a culled/upward/degenerate triangle");
				}
			}
		}
	}

	@Test
	void haloIsAnAnnulusAndCrownHasAuthoredAngularGaps() {
		LightRealmSkyGeometry.Mesh halo = LightRealmSkyGeometry.build(LightRealmSkyProfile.Shape.OUTER_HALO);
		assertTrue(minRadius(halo.vertices()) >= 86.0, "halo must retain a visible empty centre");
		assertTrue(maxRadius(halo.vertices()) >= 117.0);

		LightRealmSkyGeometry.Mesh crown = LightRealmSkyGeometry.build(LightRealmSkyProfile.Shape.CROWN_ARCS);
		assertEquals(12, crown.drawRanges().size());
		assertTrue(maxAngularGap(crown.vertices()) > Math.toRadians(30.0),
				"separate crown arcs require visible gaps rather than one filled fan");
	}

	@Test
	void compassAndVeilUseSeparatedRadialArmsWithoutFilledCentres() {
		LightRealmSkyGeometry.Mesh compass = LightRealmSkyGeometry.build(
				LightRealmSkyProfile.Shape.RUNIC_COMPASS);
		LightRealmSkyGeometry.Mesh veil = LightRealmSkyGeometry.build(LightRealmSkyProfile.Shape.RADIAL_VEIL);
		assertEquals(8, compass.drawRanges().size());
		assertEquals(12, veil.drawRanges().size());
		assertTrue(minRadius(compass.vertices()) >= 29.0);
		assertTrue(minRadius(veil.vertices()) >= 21.0);
	}

	@Test
	void geometryHasHardImmutableCpuAndDrawBounds() {
		assertEquals(128, LightRealmSkyGeometry.MAX_VERTICES_PER_SHAPE);
		assertEquals(32, LightRealmSkyGeometry.MAX_DRAWS_PER_SHAPE);
		assertEquals(256, LightRealmSkyGeometry.MAX_TOTAL_VERTICES);
		assertEquals(64, LightRealmSkyGeometry.MAX_TOTAL_DRAWS);
		int vertices = 0;
		int draws = 0;
		for (LightRealmSkyProfile.Shape shape : LightRealmSkyProfile.Shape.values()) {
			LightRealmSkyGeometry.Mesh mesh = LightRealmSkyGeometry.build(shape);
			vertices += mesh.vertices().size();
			draws += mesh.drawRanges().size();
			assertTrue(mesh.vertices().size() <= LightRealmSkyGeometry.MAX_VERTICES_PER_SHAPE);
			assertTrue(mesh.drawRanges().size() <= LightRealmSkyGeometry.MAX_DRAWS_PER_SHAPE);
			assertThrowsUnsupported(mesh.vertices(), mesh.drawRanges());
		}
		assertTrue(vertices <= LightRealmSkyGeometry.MAX_TOTAL_VERTICES);
		assertTrue(draws <= LightRealmSkyGeometry.MAX_TOTAL_DRAWS);
	}

	private static double minRadius(List<LightRealmSkyGeometry.Vertex> vertices) {
		return vertices.stream().mapToDouble(vertex -> Math.hypot(vertex.x(), vertex.z())).min().orElseThrow();
	}

	private static double maxRadius(List<LightRealmSkyGeometry.Vertex> vertices) {
		return vertices.stream().mapToDouble(vertex -> Math.hypot(vertex.x(), vertex.z())).max().orElseThrow();
	}

	private static double maxAngularGap(List<LightRealmSkyGeometry.Vertex> vertices) {
		List<Double> angles = vertices.stream().map(vertex -> {
			double angle = Math.atan2(vertex.x(), vertex.z());
			return angle < 0.0 ? angle + Math.PI * 2.0 : angle;
		}).distinct().sorted().toList();
		double result = angles.get(0) + Math.PI * 2.0 - angles.get(angles.size() - 1);
		for (int index = 1; index < angles.size(); index++) {
			result = Math.max(result, angles.get(index) - angles.get(index - 1));
		}
		return result;
	}

	private static void assertThrowsUnsupported(List<?> vertices, List<?> ranges) {
		org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, vertices::clear);
		org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, ranges::clear);
	}
}

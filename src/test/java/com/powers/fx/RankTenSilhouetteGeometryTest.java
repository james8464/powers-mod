package com.powers.fx;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves that shared silhouette expansion stays finite, bounded, and accessibility-stable. */
class RankTenSilhouetteGeometryTest {
	private static final RankTenSilhouetteGeometry.Camera CAMERA =
			new RankTenSilhouetteGeometry.Camera(20, 80, -12);

	@Test
	void everyProfileExpandsToAFiniteCappedMeshAndKeepsItsOwnPrimitiveSignature() {
		for (String id : RankTenSilhouetteProfile.powerIds()) {
			RankTenSilhouetteProfile profile = RankTenSilhouetteProfile.forPower(id).orElseThrow();
			RankTenSilhouetteGeometry.Mesh mesh = RankTenSilhouetteGeometry.mesh(profile,
					event(profile.networkId(), 1.25), CAMERA, false);
			assertEquals(profile.primitiveSignature(), mesh.primitiveSignature(), id);
			assertTrue(mesh.vertices().size() > 0 && mesh.vertices().size() <= 256, id);
			assertTrue(mesh.vertices().stream().allMatch(RankTenSilhouetteGeometry.Vertex::finite), id);
		}
	}

	@Test
	void reducedMotionFreezesPhaseAndLowersOnlyFillNotTheOuterOutline() {
		RankTenSilhouetteProfile profile = RankTenSilhouetteProfile.forPower("time_shift").orElseThrow();
		RankTenSilhouetteGeometry.Mesh normal = RankTenSilhouetteGeometry.mesh(profile,
				event(profile.networkId(), 1.25), CAMERA, false);
		RankTenSilhouetteGeometry.Mesh reduced = RankTenSilhouetteGeometry.mesh(profile,
				event(profile.networkId(), 7.75), CAMERA, true);
		assertEquals(normal.outerOutlineSignature(), reduced.outerOutlineSignature());
		assertEquals(0.0, reduced.phase());
		assertTrue(reduced.fillAlpha() < normal.fillAlpha());
		assertFalse(normal.vertices().equals(reduced.vertices()));
	}

	@Test
	void invalidEventsOrCameraCannotReachGeometryAndScaleIsClamped() {
		RankTenSilhouetteProfile profile = RankTenSilhouetteProfile.forPower("flight").orElseThrow();
		assertThrows(IllegalArgumentException.class, () -> new RankTenSilhouetteGeometry.Camera(
				Double.NaN, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> new RankTenSilhouetteGeometry.Event(
				1, profile.networkId(), UUID.randomUUID(), "minecraft:overworld", Double.NaN, 1, 2,
				0, 0, 0, 1, 40, 0));
		assertEquals(0.25, RankTenSilhouetteGeometry.clampScale(-10));
		assertEquals(8.0, RankTenSilhouetteGeometry.clampScale(100));
		assertEquals(1.0, RankTenSilhouetteGeometry.clampScale(1.0));
	}

	private static RankTenSilhouetteGeometry.Event event(int profileId, double phase) {
		return new RankTenSilhouetteGeometry.Event(8, profileId, UUID.fromString(
				"00000000-0000-0000-0000-000000000008"), "minecraft:overworld",
				5, 72, -4, 35, -12, 0, 17, 40, phase);
	}
}

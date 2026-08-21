package com.powers.visual;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightRealmSkyRulesTest {
	@Test
	void nonLightRealmNeverClaimsTheVanillaSky() {
		LightRealmSkyProfile profile = LightRealmSkyRules.resolve(false, false, true, 6_000.5);

		assertEquals(LightRealmSkyProfile.Mode.NONE, profile.mode());
		assertTrue(profile.layers().isEmpty());
	}

	@Test
	void unavailableEnhancementRetainsExactStaticWhiteFallback() {
		LightRealmSkyProfile profile = LightRealmSkyRules.resolve(true, false, false, 6_000.5);

		assertEquals(LightRealmSkyProfile.Mode.STATIC_WHITE, profile.mode());
		assertEquals(0xFFFFFFFF, profile.baseColor());
		assertTrue(profile.layers().isEmpty());
		assertFalse(profile.usesTexture());
		assertFalse(profile.usesCustomShader());
	}

	@Test
	void ordinaryProfileUsesDistinctBoundedAncientGeometryWithoutAssetDependencies() {
		LightRealmSkyProfile profile = LightRealmSkyRules.resolve(true, false, true, 6_000.5);

		assertEquals(LightRealmSkyProfile.Mode.ANCIENT_WHITE, profile.mode());
		assertEquals(0xFFFFFFFF, profile.baseColor());
		assertEquals(4, profile.layers().size());
		assertEquals(4, new HashSet<>(profile.layers().stream().map(
				LightRealmSkyProfile.Layer::shape).toList()).size());
		assertEquals(List.of(0xFFFFF8DE, 0xFFFFE5A6, 0xFFFFF0C7, 0xFFFFD77A),
				profile.layers().stream().map(LightRealmSkyProfile.Layer::color).toList());
		assertTrue(profile.layers().stream().allMatch(layer -> layer.alpha() >= 0.04 && layer.alpha() <= 0.22));
		assertTrue(profile.layers().stream().allMatch(layer -> layer.scale() >= 0.35 && layer.scale() <= 1.0));
		assertTrue(profile.layers().stream().anyMatch(layer -> Math.abs(layer.angularVelocity()) > 0.0));
		assertFalse(profile.usesTexture());
		assertFalse(profile.usesCustomShader());
	}

	@Test
	void reducedMotionPreservesLargeSilhouetteButRemovesAnimationAndPulse() {
		LightRealmSkyProfile normal = LightRealmSkyRules.resolve(true, false, true, 6_000.5);
		LightRealmSkyProfile reduced = LightRealmSkyRules.resolve(true, true, true, 6_000.5);

		assertEquals(LightRealmSkyProfile.Mode.ANCIENT_WHITE_REDUCED, reduced.mode());
		assertEquals(2, reduced.layers().size());
		assertEquals(normal.layers().get(0).shape(), reduced.layers().get(0).shape());
		assertEquals(normal.layers().get(1).shape(), reduced.layers().get(1).shape());
		assertTrue(reduced.layers().stream().allMatch(layer -> layer.angularVelocity() == 0.0));
		assertTrue(reduced.layers().stream().allMatch(layer -> layer.pulseAmplitude() == 0.0));
		assertTrue(reduced.layers().stream().mapToDouble(LightRealmSkyProfile.Layer::alpha).sum()
				< normal.layers().stream().mapToDouble(LightRealmSkyProfile.Layer::alpha).sum());
	}

	@Test
	void malformedTimeCannotCreateNonFiniteFrameValues() {
		for (double time : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
			LightRealmSkyProfile profile = LightRealmSkyRules.resolve(true, false, true, time);
			assertTrue(Double.isFinite(profile.rotationRadians()));
			assertTrue(profile.layers().stream().allMatch(layer -> Double.isFinite(layer.phase())));
		}
	}

	@Test
	void profileLayersCannotBeMutatedAfterResolution() {
		LightRealmSkyProfile profile = LightRealmSkyRules.resolve(true, false, true, 6_000.5);
		assertThrows(UnsupportedOperationException.class, () -> profile.layers().clear());
	}

	@Test
	void publicProfilesCannotEnableTextureOrCustomShaderDependencies() {
		assertThrows(IllegalArgumentException.class, () -> new LightRealmSkyProfile(
				LightRealmSkyProfile.Mode.STATIC_WHITE, 0xFFFFFFFF, List.of(), 0.0, true, false));
		assertThrows(IllegalArgumentException.class, () -> new LightRealmSkyProfile(
				LightRealmSkyProfile.Mode.STATIC_WHITE, 0xFFFFFFFF, List.of(), 0.0, false, true));
	}

	@Test
	void publicProfilesPreserveModeAndExactWhiteFallbackInvariants() {
		LightRealmSkyProfile.Layer valid = layer(0xFFFFE5A6, 0.1, 0.8, 0.0, 0.0, 0.0);
		assertThrows(IllegalArgumentException.class, () -> new LightRealmSkyProfile(
				LightRealmSkyProfile.Mode.NONE, 0, List.of(valid), 0.0, false, false));
		assertThrows(IllegalArgumentException.class, () -> new LightRealmSkyProfile(
				LightRealmSkyProfile.Mode.STATIC_WHITE, 0xFFF0F0F0, List.of(), 0.0, false, false));
		assertThrows(IllegalArgumentException.class, () -> new LightRealmSkyProfile(
				LightRealmSkyProfile.Mode.STATIC_WHITE, 0xFFFFFFFF, List.of(valid), 0.0, false, false));
		assertThrows(IllegalArgumentException.class, () -> new LightRealmSkyProfile(
				LightRealmSkyProfile.Mode.ANCIENT_WHITE, 0xFFFFF8DE, List.of(valid), 0.0, false, false));
	}

	@Test
	void publicLayersRejectValuesOutsideRendererSafeBounds() {
		assertThrows(IllegalArgumentException.class, () -> layer(0xFFFFE5A6, 0.26, 0.8, 0.0, 0.0, 0.0));
		assertThrows(IllegalArgumentException.class, () -> layer(0xFFFFE5A6, 0.039, 0.8, 0.0, 0.0, 0.0));
		assertThrows(IllegalArgumentException.class, () -> layer(0xFFFFE5A6, 0.1, 0.34, 0.0, 0.0, 0.0));
		assertThrows(IllegalArgumentException.class, () -> layer(0xFFFFE5A6, 0.1, 1.01, 0.0, 0.0, 0.0));
		assertThrows(IllegalArgumentException.class, () -> layer(0xFFFFE5A6, 0.1, 0.8, 0.0011, 0.0, 0.0));
		assertThrows(IllegalArgumentException.class, () -> layer(0xFFFFE5A6, 0.1, 0.8, -0.0011, 0.0, 0.0));
		assertThrows(IllegalArgumentException.class, () -> layer(0xFFFFE5A6, 0.1, 0.8, 0.0, 0.051, 0.0));
		assertThrows(IllegalArgumentException.class, () -> layer(0xFFFFE5A6, 0.1, 0.8, 0.0, -0.001, 0.0));
		assertThrows(IllegalArgumentException.class, () -> layer(0xFFFFE5A6, 0.1, 0.8, 0.0, 0.0,
				Math.PI * 2.0 + 0.01));
		assertThrows(IllegalArgumentException.class, () -> layer(0x80FFE5A6, 0.1, 0.8, 0.0, 0.0, 0.0));
	}

	private static LightRealmSkyProfile.Layer layer(int color, double alpha, double scale,
			double velocity, double pulse, double phase) {
		return new LightRealmSkyProfile.Layer(LightRealmSkyProfile.Shape.OUTER_HALO,
				color, alpha, scale, velocity, pulse, phase);
	}
}

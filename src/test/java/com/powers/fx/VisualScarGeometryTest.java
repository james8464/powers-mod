package com.powers.fx;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualScarGeometryTest {
	@Test
	void allThirtyProfilesOnSixFacesProduceBoundedFiniteSurfaceMeshesAtWorldLimits() {
		for (var profile : VisualScarPresentation.allProfiles()) {
			for (VisualScarRules.Face face : VisualScarRules.Face.values()) {
				var mesh = mesh(profile, face, VisualScarMotifGeometry.Lod.NEAR, 91);
				assertEquals(profile, mesh.profile());
				assertEquals(profile.motif(), mesh.motif());
				assertTrue(mesh.quads().size() > 0 && mesh.quads().size() <= 16);
				assertEquals(mesh.quads().size() * 4, mesh.vertices().size());
				assertTrue(mesh.vertices().size() <= 64);
				Set<Integer> colors = mesh.vertices().stream()
						.map(VisualScarMotifGeometry.Vertex::rgba).collect(Collectors.toSet());
				assertTrue(colors.contains(pack(profile.materialBaseRgb(), profile.alpha())));
				assertTrue(colors.contains(pack(profile.accentRgb(), profile.alpha())));
				for (var quad : mesh.quads()) {
					assertEquals(4, quad.vertices().size());
					assertTrue(quad.vertices().stream().allMatch(
							VisualScarMotifGeometry.Vertex::finite));
					assertTrue(quad.area() > 0.0);
					assertTrue(quad.outwardOffset() > 0.0 && quad.outwardOffset() <= 0.01);
					assertTrue(quad.withinScarBounds());
					assertTrue(quad.surfaceAligned(face));
					assertTrue(quad.windingFacesOutward(face));
				}
			}
		}
	}

	@Test
	void presentationIsAClosedUniqueFiveBySixBoundedProfileMatrix() {
		assertEquals(Set.of(VisualScarPresentation.Motif.LINEAR_RUNE,
				VisualScarPresentation.Motif.RADIAL_CRACK,
				VisualScarPresentation.Motif.FORKED_WAVE,
				VisualScarPresentation.Motif.FROST_BRANCH,
				VisualScarPresentation.Motif.EMBER_RING),
				Set.of(VisualScarPresentation.Motif.values()));
		assertEquals(5, VisualScarRules.Impact.values().length);
		assertEquals(6, VisualScarRules.Material.values().length);
		assertEquals(VisualScarPresentation.Motif.LINEAR_RUNE,
				VisualScarPresentation.motif(VisualScarRules.Impact.BEAM));
		assertEquals(VisualScarPresentation.Motif.RADIAL_CRACK,
				VisualScarPresentation.motif(VisualScarRules.Impact.SLAM));
		assertEquals(VisualScarPresentation.Motif.FORKED_WAVE,
				VisualScarPresentation.motif(VisualScarRules.Impact.THUNDERCLAP));
		assertEquals(VisualScarPresentation.Motif.FROST_BRANCH,
				VisualScarPresentation.motif(VisualScarRules.Impact.ICE));
		assertEquals(VisualScarPresentation.Motif.EMBER_RING,
				VisualScarPresentation.motif(VisualScarRules.Impact.FIRE));
		var profiles = VisualScarPresentation.allProfiles();
		assertEquals(30, profiles.size());
		assertEquals(30, profiles.stream().map(VisualScarPresentation.Profile::key)
				.collect(Collectors.toSet()).size());
		assertEquals(30, profiles.stream().collect(Collectors.toSet()).size());
		assertEquals(6, profiles.stream().map(VisualScarPresentation.Profile::materialBaseRgb)
				.collect(Collectors.toSet()).size());
		assertEquals(5, profiles.stream().map(VisualScarPresentation.Profile::motif)
				.collect(Collectors.toSet()).size());
		for (var impact : VisualScarRules.Impact.values()) {
			for (var material : VisualScarRules.Material.values()) {
				var profile = VisualScarPresentation.profile(impact, material);
				assertEquals(new VisualScarPresentation.Key(impact, material), profile.key());
				assertTrue(profile.materialBaseRgb() >= 0 && profile.materialBaseRgb() <= 0xFFFFFF);
				assertTrue(profile.accentRgb() >= 0 && profile.accentRgb() <= 0xFFFFFF);
				assertTrue(profile.alpha() >= 0.15 && profile.alpha() <= 0.95);
				assertTrue(profile.segments() >= 3 && profile.segments() <= 24);
				assertTrue(profile.stroke() >= 0.01 && profile.stroke() <= 0.20);
				assertTrue(profile.inset() >= 0.0 && profile.inset() <= 0.40);
				assertTrue(profile.variation() >= 0.0 && profile.variation() <= 1.0);
				assertFalse(profile.usesTexture());
				assertFalse(profile.usesCustomShader());
			}
		}
		var authored = profiles.getFirst();
		assertThrows(IllegalArgumentException.class, () -> new VisualScarPresentation.Profile(
				authored.key(), authored.motif(), authored.materialBaseRgb(), authored.accentRgb(),
				authored.alpha(), authored.segments(), authored.stroke(), authored.inset(),
				authored.variation(), true, false));
	}

	@Test
	void motifsHaveDistinctTopologyAndEveryGeometryParameterChangesVertices() {
		for (var lod : VisualScarMotifGeometry.Lod.values()) {
			var signatures = java.util.Arrays.stream(VisualScarRules.Impact.values())
					.map(impact -> mesh(VisualScarPresentation.profile(
							impact, VisualScarRules.Material.STONE), VisualScarRules.Face.UP, lod, 7)
							.topologySignature())
					.collect(Collectors.toSet());
			assertEquals(5, signatures.size());
		}

		var profile = VisualScarPresentation.profile(
				VisualScarRules.Impact.BEAM, VisualScarRules.Material.STONE);
		String baseline = mesh(profile, VisualScarRules.Face.UP,
				VisualScarMotifGeometry.Lod.NEAR, 11).geometryDigest();
		int segments = profile.segments() == 24 ? 23 : profile.segments() + 1;
		double stroke = profile.stroke() > 0.02 ? profile.stroke() - 0.01 : 0.02;
		double inset = profile.inset() < 0.39 ? profile.inset() + 0.01 : 0.38;
		double variation = profile.variation() < 0.5 ? 0.75 : 0.25;
		assertFalse(baseline.equals(mesh(profile.withSegments(segments), VisualScarRules.Face.UP,
				VisualScarMotifGeometry.Lod.NEAR, 11).geometryDigest()));
		assertFalse(baseline.equals(mesh(profile.withStroke(stroke), VisualScarRules.Face.UP,
				VisualScarMotifGeometry.Lod.NEAR, 11).geometryDigest()));
		assertFalse(baseline.equals(mesh(profile.withInset(inset), VisualScarRules.Face.UP,
				VisualScarMotifGeometry.Lod.NEAR, 11).geometryDigest()));
		assertFalse(baseline.equals(mesh(profile.withVariation(variation), VisualScarRules.Face.UP,
				VisualScarMotifGeometry.Lod.NEAR, 11).geometryDigest()));
		assertFalse(baseline.equals(mesh(profile.withMaterialBaseRgb(0x010203),
				VisualScarRules.Face.UP, VisualScarMotifGeometry.Lod.NEAR, 11).geometryDigest()));
		assertFalse(baseline.equals(mesh(profile.withAccentRgb(0xFDFCFB),
				VisualScarRules.Face.UP, VisualScarMotifGeometry.Lod.NEAR, 11).geometryDigest()));
		assertFalse(baseline.equals(mesh(profile.withAlpha(0.33),
				VisualScarRules.Face.UP, VisualScarMotifGeometry.Lod.NEAR, 11).geometryDigest()));
		for (var vertex : mesh(profile, VisualScarRules.Face.UP,
				VisualScarMotifGeometry.Lod.NEAR, 11).vertices()) {
			assertTrue(vertex.rgba() != 0);
			int alpha = vertex.rgba() & 0xFF;
			assertTrue(alpha >= 38 && alpha <= 242);
		}
	}

	@Test
	void nearMidFarLodRetainsRecognisableMotifsWithinMonotonicBudgets() {
		assertEquals(VisualScarMotifGeometry.Lod.NEAR,
				VisualScarMotifGeometry.lodForDistance(48));
		assertEquals(VisualScarMotifGeometry.Lod.MID,
				VisualScarMotifGeometry.lodForDistance(49));
		assertEquals(VisualScarMotifGeometry.Lod.MID,
				VisualScarMotifGeometry.lodForDistance(128));
		assertEquals(VisualScarMotifGeometry.Lod.FAR,
				VisualScarMotifGeometry.lodForDistance(129));
		assertEquals(VisualScarMotifGeometry.Lod.FAR,
				VisualScarMotifGeometry.lodForDistance(256));
		for (var impact : VisualScarRules.Impact.values()) {
			var profile = VisualScarPresentation.profile(impact, VisualScarRules.Material.METAL);
			var near = mesh(profile, VisualScarRules.Face.NORTH, VisualScarMotifGeometry.Lod.NEAR, 1);
			var mid = mesh(profile, VisualScarRules.Face.NORTH, VisualScarMotifGeometry.Lod.MID, 1);
			var far = mesh(profile, VisualScarRules.Face.NORTH, VisualScarMotifGeometry.Lod.FAR, 1);
			assertTrue(near.quads().size() <= 16);
			assertTrue(mid.quads().size() > 0 && mid.quads().size() <= 10);
			assertTrue(far.quads().size() > 0 && far.quads().size() <= 6);
			assertTrue(far.quads().size() <= mid.quads().size());
			assertTrue(mid.quads().size() <= near.quads().size());
			assertEquals(profile.motif(), near.recognisableSilhouette());
			assertEquals(profile.motif(), mid.recognisableSilhouette());
			assertEquals(profile.motif(), far.recognisableSilhouette());
			assertTrue(near.recognitionAnchors() > 0);
			assertTrue(mid.recognitionAnchors() > 0);
			assertTrue(far.recognitionAnchors() > 0);
		}
	}

	@Test
	void debugQuadsPipelineAndSingleBatchHaveExactTopologyDepthAndCullContract() {
		var pipeline = VisualScarMotifGeometry.pipelineContract();
		assertEquals("DEBUG_QUADS", pipeline.pipelineName());
		assertEquals("POSITION_COLOR", pipeline.vertexFormat());
		assertEquals("QUADS", pipeline.topology());
		assertTrue(pipeline.translucent());
		assertFalse(pipeline.cull());
		assertTrue(pipeline.reverseDepthTest());
		assertFalse(pipeline.depthWrite());

		var profile = VisualScarPresentation.profile(
				VisualScarRules.Impact.FIRE, VisualScarRules.Material.EARTH);
		var visible = new java.util.ArrayList<VisualScarMotifGeometry.Candidate>();
		for (long key = 0; key < 10_000; key++) {
			visible.add(new VisualScarMotifGeometry.Candidate(key, key < 511 ? key : 1_000));
		}
		visible.add(new VisualScarMotifGeometry.Candidate(900, 511));
		visible.add(new VisualScarMotifGeometry.Candidate(800, 511));
		AtomicInteger meshCalls = new AtomicInteger();
		AtomicInteger attemptedVertices = new AtomicInteger();
		var batch = VisualScarMotifGeometry.batchNearestCandidates(visible, 512, 8_192, 32_768,
				candidate -> {
					meshCalls.incrementAndGet();
					var generated = mesh(profile, VisualScarRules.Face.UP,
							VisualScarMotifGeometry.Lod.NEAR, candidate.key());
					attemptedVertices.addAndGet(generated.vertices().size());
					return generated;
				});
		assertEquals(1, batch.drawCalls());
		assertEquals(512, batch.scarCount());
		assertTrue(meshCalls.get() <= 512);
		assertTrue(attemptedVertices.get() <= 32_768);
		assertTrue(batch.quadCount() <= 8_192);
		assertTrue(batch.vertices().size() <= 32_768);
		assertTrue(batch.selectedKeys().contains(800L));
		assertFalse(batch.selectedKeys().contains(900L));
		assertFalse(batch.selectedKeys().contains(999L));
		assertTrue(batch.vertices().stream().allMatch(vertex -> vertex.rgba() != 0));
	}

	@Test
	void visibilityHidesWithoutDeletingSemanticRecord() {
		assertEquals(VisualScarMotifGeometry.Visibility.VISIBLE,
				VisualScarMotifGeometry.visibility(20, true, true, true));
		assertEquals(VisualScarMotifGeometry.Visibility.HIDE_RANGE,
				VisualScarMotifGeometry.visibility(257, true, true, true));
		assertEquals(VisualScarMotifGeometry.Visibility.HIDE_FRUSTUM,
				VisualScarMotifGeometry.visibility(20, false, true, true));
		assertEquals(VisualScarMotifGeometry.Visibility.HIDE_UNLOADED,
				VisualScarMotifGeometry.visibility(20, true, false, true));
		assertEquals(VisualScarMotifGeometry.Visibility.HIDE_SUPPORT,
				VisualScarMotifGeometry.visibility(20, true, true, false));

		var state = ClientVisualScarState.empty(2_048, 7).receive(
				new ScarFxProtocolRules.Wire(0, 42, 1, 0, 0, 9, 1, 40), 0, 7);
		assertFalse(state.visible(42, 1, false, false));
		assertEquals(1, state.size());
	}

	private static VisualScarMotifGeometry.Mesh mesh(VisualScarPresentation.Profile profile,
			VisualScarRules.Face face, VisualScarMotifGeometry.Lod lod, long seed) {
		return VisualScarMotifGeometry.mesh(profile, face,
				29_999_999.0, 200.0, -29_999_999.0,
				29_999_990.25, 198.5, -29_999_990.75,
				0.002, 0.82, seed, lod);
	}

	private static int pack(int rgb, double alpha) {
		return rgb << 8 | (int) Math.round(alpha * 255.0);
	}
}

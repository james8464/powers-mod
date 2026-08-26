package com.powers.fx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the production client ownership, batching, and accessibility boundaries. */
class RankTenSilhouetteRendererSourceTest {
	private static final Path MANAGER = Path.of(
			"src/client/java/com/powers/client/fx/ClientRankTenSilhouetteManager.java");
	private static final Path RENDERER = Path.of(
			"src/client/java/com/powers/client/fx/ClientRankTenSilhouetteRenderer.java");
	private static final Path CLIENT = Path.of("src/client/java/com/powers/client/PowersClient.java");
	private static final String OVERWORLD = "minecraft:overworld";
	private static final String NETHER = "minecraft:the_nether";

	@Test
	void dimensionGenerationRejectsAbaCallbacksAndPreservesTheConnectionReplayWatermark() {
		RankTenSilhouetteClientOwnership owner = RankTenSilhouetteClientOwnership.empty(7, OVERWORLD);
		RankTenSilhouetteClientOwnership.HandlerStamp staleA = owner.stamp();
		owner = owner.observeDimension(NETHER).observeDimension(OVERWORLD);
		assertFalse(owner.canAccept(staleA, 1), "A callback must not survive A to B to A");

		RankTenSilhouetteClientOwnership.HandlerStamp currentA = owner.stamp();
		assertTrue(owner.canAccept(currentA, 9));
		owner = owner.accept(currentA, 9).observeDimension(NETHER).observeDimension(OVERWORLD);
		assertEquals(9, owner.latestAcceptedEventId());
		assertFalse(owner.canAccept(owner.stamp(), 9),
				"Dimension reset must not resurrect an accepted event ID");
	}

	@Test
	void sameDimensionWorldReplacementAlsoAdvancesTheGeneration() {
		RankTenSilhouetteClientOwnership owner = RankTenSilhouetteClientOwnership.empty(7, OVERWORLD);
		RankTenSilhouetteClientOwnership.HandlerStamp staleWorld = owner.stamp();
		owner = owner.advanceWorld(OVERWORLD);
		assertEquals(2, owner.dimensionGeneration());
		assertFalse(owner.canAccept(staleWorld, 1));
	}

	@Test
	void cameraRelativeExpansionKeepsThinProfileGeometryNearTheWorldBorder() {
		RankTenSilhouetteProfile profile = RankTenSilhouetteProfile.forPower("flight").orElseThrow();
		ClientRankTenSilhouetteState.Entry entry = entry(21, profile.networkId(),
				29_999_999.75, 72.0, -29_999_999.25, 40);
		RankTenSilhouetteGeometry.Camera camera = new RankTenSilhouetteGeometry.Camera(
				29_999_997.25, 71.5, -29_999_996.75);

		RankTenSilhouetteGeometry.Mesh mesh = RankTenSilhouetteRenderBatch.renderActualProfileMesh(
				entry, camera, false, 1.25);
		double minimumX = mesh.vertices().stream().mapToDouble(RankTenSilhouetteGeometry.Vertex::x)
				.min().orElseThrow();
		double maximumX = mesh.vertices().stream().mapToDouble(RankTenSilhouetteGeometry.Vertex::x)
				.max().orElseThrow();
		assertTrue(maximumX - minimumX > 0.1, "Thin authored ribbons collapsed after float conversion");
		assertTrue(mesh.vertices().stream().allMatch(vertex -> Math.abs(vertex.x()) < 16
				&& Math.abs(vertex.y()) < 16 && Math.abs(vertex.z()) < 16),
				"Production mesh must already be camera-relative before conversion to float");
		assertEquals(profile.primitiveSignature(), mesh.primitiveSignature());
	}

	@Test
	void productionMeshKeepsSegmentAndRingMinorWidthRecognisableAtNinetySixBlocks() {
		double distance = 96.0;
		double focalPixels = 720.0 / (2.0 * Math.tan(Math.toRadians(70.0 / 2.0)));
		assertEquals(0.12, RankTenSilhouetteRenderBatch.distanceStableMinorWidth(0.12, 8));
		assertEquals(0.336, RankTenSilhouetteRenderBatch.distanceStableMinorWidth(0.12, distance),
				1.0E-12);
		assertEquals(0.36, RankTenSilhouetteRenderBatch.distanceStableMinorWidth(0.12, 384));
		RankTenSilhouetteGeometry.Camera farCamera = new RankTenSilhouetteGeometry.Camera(0, 70, -distance);
		RankTenSilhouetteGeometry.Camera nearCamera = new RankTenSilhouetteGeometry.Camera(0, 70, -8);
		for (String powerId : List.of("invisibility", "time_shift")) {
			RankTenSilhouetteProfile profile = RankTenSilhouetteProfile.forPower(powerId).orElseThrow();
			ClientRankTenSilhouetteState.Entry entry = entry(31 + profile.networkId(),
					profile.networkId(), 0, 70, 0, 40);
			RankTenSilhouetteGeometry.Mesh far = RankTenSilhouetteRenderBatch.renderActualProfileMesh(
					entry, farCamera, false, 0);
			RankTenSilhouetteGeometry.Mesh authoredFar = RankTenSilhouetteGeometry.mesh(profile,
					relativeEvent(entry, farCamera, 0), new RankTenSilhouetteGeometry.Camera(0, 0, 0), false);
			double projectedMinorPixels = minorWidth(far) * focalPixels / distance;
			assertTrue(projectedMinorPixels >= 1.5, powerId + " projected only "
					+ projectedMinorPixels + " pixels at 96 blocks");
			assertMidpointClose(authoredFar.vertices().get(0), authoredFar.vertices().get(1),
					far.vertices().get(0), far.vertices().get(1), powerId);
			assertTrue(far.vertices().size() <= RankTenSilhouetteGeometry.MAX_VERTICES);

			RankTenSilhouetteGeometry.Mesh near = RankTenSilhouetteRenderBatch.renderActualProfileMesh(
					entry, nearCamera, false, 0);
			RankTenSilhouetteGeometry.Mesh authoredNear = RankTenSilhouetteGeometry.mesh(profile,
					relativeEvent(entry, nearCamera, 0), new RankTenSilhouetteGeometry.Camera(0, 0, 0), false);
			assertEquals(authoredNear.vertices(), near.vertices(), powerId + " near geometry changed");
		}
	}

	@Test
	void pureBatchSelectsNearestWithStableTiesAndHardCapsOneDraw() {
		RankTenSilhouetteGeometry.Camera camera = new RankTenSilhouetteGeometry.Camera(0, 64, 0);
		List<ClientRankTenSilhouetteState.Entry> entries = List.of(
				entry(30, 2, 12, 64, 0, 40),
				entry(20, 5, 3, 64, 4, 40),
				entry(10, 6, -3, 64, -4, 40));

		RankTenSilhouetteRenderBatch.Batch batch = RankTenSilhouetteRenderBatch.batch(
				entries, camera, false, 17, 2, 512);
		assertEquals(List.of(10L, 20L), batch.eventIds());
		assertEquals(2, batch.silhouetteCount());
		assertEquals(1, batch.drawCalls());
		assertTrue(batch.vertices().size() <= 512);
		assertEquals(0, batch.vertices().size() % 4);
	}

	@Test
	void realProfilePathUsesLifecycleAndSeedButReducedMotionFreezesPhaseAndFill() {
		RankTenSilhouetteProfile profile = RankTenSilhouetteProfile.forPower("fireball").orElseThrow();
		ClientRankTenSilhouetteState.Entry entry = entry(5, profile.networkId(), 4, 70, 2, 40);
		RankTenSilhouetteGeometry.Camera camera = new RankTenSilhouetteGeometry.Camera(0, 70, 0);
		RankTenSilhouetteRenderBatch.Batch normal = RankTenSilhouetteRenderBatch.batch(
				List.of(entry), camera, false, 20, 1, 512);
		RankTenSilhouetteRenderBatch.Batch reduced = RankTenSilhouetteRenderBatch.batch(
				List.of(entry), camera, true, 20, 1, 512);

		assertEquals((20 + Integer.toUnsignedLong(entry.wire().visualSeed())) * 0.12,
				normal.meshes().getFirst().phase());
		assertEquals(0.0, reduced.meshes().getFirst().phase());
		assertEquals(normal.meshes().getFirst().outerOutlineSignature(),
				reduced.meshes().getFirst().outerOutlineSignature());
		assertTrue(reduced.meshes().getFirst().fillAlpha() < normal.meshes().getFirst().fillAlpha());
	}

	@Test
	void purePipelineContractMatchesTheDepthTestedDebugQuadSubmission() {
		RankTenSilhouetteRenderBatch.PipelineContract pipeline =
				RankTenSilhouetteRenderBatch.pipelineContract();
		assertEquals("DEBUG_QUADS", pipeline.pipelineName());
		assertEquals("POSITION_COLOR", pipeline.vertexFormat());
		assertEquals("QUADS", pipeline.topology());
		assertTrue(pipeline.translucent());
		assertFalse(pipeline.cull());
		assertTrue(pipeline.reverseDepthTest());
		assertFalse(pipeline.depthWrite());
	}

	@Test
	void packetHandlerCapturesOwnershipBeforeEnqueueAndUsesTheAcceptedStateBoundary()
			throws IOException {
		String manager = Files.readString(MANAGER);
		String client = Files.readString(CLIENT);
		String receiver = block(client,
				"ClientPlayNetworking.registerGlobalReceiver(RankTenSilhouettePackets.Payload.TYPE");

		int capture = receiver.indexOf("ClientRankTenSilhouetteManager.captureHandlerStamp(context.client())");
		int enqueue = receiver.indexOf("context.client().execute(");
		assertTrue(capture >= 0 && enqueue > capture,
				"Connection/dimension ownership must be captured on receipt before enqueue");
		assertTrue(receiver.contains("ClientRankTenSilhouetteManager.handle(payload, captured)"));
		assertTrue(manager.contains("ClientRankTenSilhouetteState.empty(CAPACITY"));
		assertTrue(manager.contains("private static final int CAPACITY = 64;"));
		assertTrue(manager.contains("ownership.canAccept(captured, payload.eventId())"));
		assertTrue(manager.contains("state.receive(payload.wire()"));
	}

	@Test
	void lifecycleWiringClearsConnectionAndDimensionButReloadPreservesLogicalEntries()
			throws IOException {
		String manager = Files.readString(MANAGER);
		String renderer = Files.readString(RENDERER);
		String client = Files.readString(CLIENT);

		assertTrue(manager.contains("state = state.tick();"));
		assertTrue(manager.contains("state = state.reset(ownership.connectionEpoch(), currentDimension);"));
		assertTrue(manager.contains("resetConnectionEpoch()"));
		String disconnect = block(client, "ClientPlayConnectionEvents.DISCONNECT.register");
		assertTrue(disconnect.contains("ClientRankTenSilhouetteManager.resetConnectionEpoch();"));
		assertTrue(disconnect.contains("ClientRankTenSilhouetteRenderer.closeResources();"));
		assertTrue(client.contains("ClientRankTenSilhouetteRenderer.recreateResources()"));
		assertTrue(client.contains("ClientRankTenSilhouetteManager.tick(client);"));
		String reload = block(client, "public void onResourceManagerReload(ResourceManager manager)");
		assertTrue(reload.contains("ClientRankTenSilhouetteRenderer.closeResources();"));
		assertTrue(reload.contains("ClientRankTenSilhouetteRenderer.recreateResources();"));
		assertTrue(renderer.contains("ClientRankTenSilhouetteManager.rendererResourcesClosed();"));
		assertTrue(renderer.contains("ClientRankTenSilhouetteManager.rendererResourcesRecreated();"));
	}

	@Test
	void rendererSubmitsOneNearestFirstCappedDepthTestedCameraRelativeBatch() throws IOException {
		String source = Files.readString(RENDERER);

		assertTrue(source.contains("LevelRenderEvents.COLLECT_SUBMITS.register"));
		assertTrue(source.contains("RankTenSilhouetteRenderBatch.batch("));
		assertTrue(source.contains("MAX_FRAME_VERTICES"));
		assertEquals(1, occurrences(source, "submitCustomGeometry("));
		assertTrue(source.contains("RenderTypes.debugQuads()"));
		assertTrue(source.contains("vertex.x(), vertex.y(), vertex.z()"));
	}

	@Test
	void testSeamRendersTheActualProductionProfileMeshAndReducedMotionIsStatic()
			throws IOException {
		String source = Files.readString(RENDERER);
		String seam = method(source, "public static RankTenSilhouetteGeometry.Mesh renderActualProfileMesh(",
				"public static void closeResources()");

		assertTrue(seam.contains("RankTenSilhouetteRenderBatch.renderActualProfileMesh("));
		assertTrue(source.contains("screenEffectScale().get()"));
		assertFalse(source.contains("options.particles()"),
				"Minimal particles must not alter or remove silhouette rendering");
	}

	@Test
	void silhouettesHaveNoParticleFovSoundOrEntityModelDependency() throws IOException {
		String source = Files.readString(MANAGER) + Files.readString(RENDERER);
		for (String forbidden : new String[] {"particleEngine", "ParticleProvider", "setFov",
				"changeFov", "SoundEvent", "playSound", "EntityRenderer", "PlayerRenderer"}) {
			assertFalse(source.contains(forbidden), "Forbidden silhouette dependency: " + forbidden);
		}
	}

	private static String method(String source, String start, String end) {
		int from = source.indexOf(start);
		int to = source.indexOf(end, from);
		assertTrue(from >= 0 && to > from, "Could not isolate method boundary");
		return source.substring(from, to);
	}

	private static ClientRankTenSilhouetteState.Entry entry(long eventId, int profileId,
			double x, double y, double z, int lifetime) {
		ClientRankTenSilhouetteState.Wire wire = new ClientRankTenSilhouetteState.Wire(eventId,
				profileId, UUID.fromString("00000000-0000-0000-0000-000000000001"), OVERWORLD,
				x, y, z, 15, -5, 0, 41, lifetime);
		return new ClientRankTenSilhouetteState.Entry(wire, lifetime);
	}

	private static RankTenSilhouetteGeometry.Event relativeEvent(
			ClientRankTenSilhouetteState.Entry entry, RankTenSilhouetteGeometry.Camera camera,
			double phase) {
		ClientRankTenSilhouetteState.Wire wire = entry.wire();
		return new RankTenSilhouetteGeometry.Event(wire.eventId(), wire.profileId(), wire.caster(),
				wire.dimension(), wire.x() - camera.x(), wire.y() - camera.y(), wire.z() - camera.z(),
				wire.yaw(), wire.pitch(), wire.alignmentId(), wire.visualSeed(), wire.lifetimeTicks(), phase);
	}

	private static double minorWidth(RankTenSilhouetteGeometry.Mesh mesh) {
		RankTenSilhouetteGeometry.Vertex first = mesh.vertices().get(0);
		RankTenSilhouetteGeometry.Vertex second = mesh.vertices().get(1);
		return Math.sqrt(Math.pow(first.x() - second.x(), 2)
				+ Math.pow(first.y() - second.y(), 2) + Math.pow(first.z() - second.z(), 2));
	}

	private static void assertMidpointClose(RankTenSilhouetteGeometry.Vertex expectedFirst,
			RankTenSilhouetteGeometry.Vertex expectedSecond,
			RankTenSilhouetteGeometry.Vertex actualFirst,
			RankTenSilhouetteGeometry.Vertex actualSecond, String powerId) {
		List<Float> expected = List.of((expectedFirst.x() + expectedSecond.x()) / 2,
				(expectedFirst.y() + expectedSecond.y()) / 2,
				(expectedFirst.z() + expectedSecond.z()) / 2);
		List<Float> actual = List.of((actualFirst.x() + actualSecond.x()) / 2,
				(actualFirst.y() + actualSecond.y()) / 2,
				(actualFirst.z() + actualSecond.z()) / 2);
		for (int axis = 0; axis < 3; axis++) {
			assertEquals(expected.get(axis), actual.get(axis), 1.0E-6, powerId + " centerline moved");
		}
	}

	private static int occurrences(String source, String needle) {
		int count = 0;
		for (int offset = 0; (offset = source.indexOf(needle, offset)) >= 0; offset += needle.length()) count++;
		return count;
	}

	private static String block(String source, String marker) {
		int markerAt = source.indexOf(marker);
		int open = markerAt < 0 ? -1 : source.indexOf('{', markerAt);
		assertTrue(open >= 0, "Could not find block for " + marker);
		int depth = 0;
		for (int index = open; index < source.length(); index++) {
			char character = source.charAt(index);
			if (character == '{') depth++;
			else if (character == '}' && --depth == 0) return source.substring(open + 1, index);
		}
		throw new AssertionError("Unclosed block for " + marker);
	}
}

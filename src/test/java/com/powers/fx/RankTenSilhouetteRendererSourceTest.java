package com.powers.fx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
		assertTrue(manager.contains("state.receive(payload.wire()"));
		assertTrue(manager.contains("captured.connectionEpoch()"));
		assertTrue(manager.contains("captured.dimension()"));
	}

	@Test
	void lifecycleWiringClearsConnectionAndDimensionButReloadPreservesLogicalEntries()
			throws IOException {
		String manager = Files.readString(MANAGER);
		String renderer = Files.readString(RENDERER);
		String client = Files.readString(CLIENT);

		assertTrue(manager.contains("state = state.tick();"));
		assertTrue(manager.contains("state = state.reset(connectionEpoch, currentDimension);"));
		assertTrue(manager.contains("public static void resetConnectionEpoch()"));
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
		assertTrue(source.contains("Comparator.comparingDouble(Candidate::distance)"));
		assertTrue(source.contains("MAX_FRAME_VERTICES"));
		assertEquals(1, occurrences(source, "submitCustomGeometry("));
		assertTrue(source.contains("RenderTypes.debugQuads()"));
		assertTrue(source.contains("vertex.x() - camera.x"));
		assertTrue(source.contains("vertex.y() - camera.y"));
		assertTrue(source.contains("vertex.z() - camera.z"));
	}

	@Test
	void testSeamRendersTheActualProductionProfileMeshAndReducedMotionIsStatic()
			throws IOException {
		String source = Files.readString(RENDERER);
		String seam = method(source, "public static RankTenSilhouetteGeometry.Mesh renderActualProfileMesh(",
				"public static void closeResources()");

		assertTrue(seam.contains("RankTenSilhouetteProfile.fromNetworkId("));
		assertTrue(seam.contains("RankTenSilhouetteGeometry.mesh(profile"));
		assertTrue(source.contains("double phase = reducedMotion ? 0.0"));
		assertTrue(source.contains("ClientRankTenSilhouetteManager.animatedPhase("));
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

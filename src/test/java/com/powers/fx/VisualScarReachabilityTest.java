package com.powers.fx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualScarReachabilityTest {
	private static final Path ROOT = Path.of("src/main/java/com/powers");
	private static final String REQUEST = "VisualScarService.request(";

	@Test
	void exactlyFiveCallersRequestAndFireGameplayRemains() throws IOException {
		List<String> requesters = new ArrayList<>();
		try (var files = Files.walk(ROOT)) {
			for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
				if (Files.readString(file).contains(REQUEST)) requesters.add(ROOT.relativize(file).toString());
			}
		}
		assertEquals(List.of("power/abilities/BreezyBashAbility.java",
				"power/abilities/EnergyBeamAbility.java",
				"power/abilities/FireballImpactResolver.java",
				"power/abilities/IceManipulationAbility.java",
				"power/abilities/ThunderclapAbility.java"), requesters.stream().sorted().toList());
		String fire = source("power/abilities/FireballImpactResolver.java");
		assertTrue(fire.contains("level.setBlockAndUpdate(pos, fire)"));
		assertTrue(fire.indexOf(REQUEST) < fire.indexOf("CombatTerrainImpact.crater"));
		String bash = source("power/abilities/BreezyBashAbility.java");
		assertTrue(bash.indexOf(REQUEST) < bash.indexOf("CombatTerrainImpact.crater"));
		String thunderclap = source("power/abilities/ThunderclapAbility.java");
		assertTrue(thunderclap.indexOf(REQUEST) < thunderclap.indexOf("CombatTerrainImpact.thunderclap"));
		String ice = source("power/abilities/IceManipulationAbility.java");
		assertTrue(ice.contains("blockHit.getDirection()"));
		assertTrue(ice.indexOf(REQUEST) < ice.indexOf(
				"for (Map.Entry<BlockPos, BlockState> mutation : mutations.entrySet())"));
		String beam = source("power/abilities/EnergyBeamAbility.java");
		assertTrue(beam.contains("ray.surfaceSupport()") && beam.contains("ray.surfaceFace()"));
		assertFalse(source("power/abilities/CombatTerrainImpact.java").contains("VisualScarService"));
	}

	@Test
	void serviceLoadsNeitherSupportNorOriginAndReadsOnlyAfterBothChecks() throws IOException {
		String service = source("fx/VisualScarService.java");
		int supportLoaded = service.indexOf("LoadedChunks.contains(level, support)");
		int originLoaded = service.indexOf("LoadedChunks.contains(level, origin)");
		int stateRead = service.indexOf("level.getBlockState(support)");
		int entityRead = service.indexOf("level.getBlockEntity(support)");
		int originStateRead = service.indexOf("level.getBlockState(origin)");
		int originEntityRead = service.indexOf("level.getBlockEntity(origin)");
		assertTrue(supportLoaded >= 0 && originLoaded >= 0);
		for (int read : List.of(stateRead, entityRead, originStateRead, originEntityRead)) {
			assertTrue(read >= 0);
			assertTrue(supportLoaded < read && originLoaded < read);
		}
		for (String forbidden : List.of("setBlock(", "setBlockAndUpdate(", "destroyBlock(",
				"SavedData", "addRegionTicket", "addTicket")) assertFalse(service.contains(forbidden));
		assertTrue(service.contains("PowerProtection.blockDecision"));
		assertTrue(service.contains("PowerProtectionAdapters.blockWorkPolicyId"));
		assertTrue(service.contains("VisualScarRequestQueue"));
		assertTrue(service.contains("pendingByKey"));
		assertTrue(service.contains("BlockWorkBudget"));
		assertTrue(service.contains("TreeMap"));
		assertTrue(service.contains("ChunkSpatialIndex"));
		assertTrue(service.contains("VisualScarLedgerRules.observeMovement"));
		assertTrue(service.contains("spatial.nearby"));
		assertTrue(service.contains("sessionPlayers.get(player.getUUID()) == player"));
		assertFalse(service.contains("System.identityHashCode"));
		assertFalse(service.contains("sessionGenerations"));
	}

	@Test
	void noWorldRegistrationAndSemanticLifecycleIsFullyReachable() throws IOException {
		assertFalse(source("PowersBlocks.java").contains("visual_scar"));
		assertFalse(source("PowersBlockEntities.java").contains("visual_scar"));
		assertFalse(source("PowersEntities.java").contains("visual_scar"));
		String packets = source("network/MagicFxPackets.java");
		assertTrue(packets.contains("ScarFxPayload.TYPE"));
		assertTrue(packets.contains("ScarFxPayload.STREAM_CODEC"));
		String client = Files.readString(Path.of("src/client/java/com/powers/client/PowersClient.java"));
		assertTrue(client.contains("ClientVisualScarManager.handle"));
		assertTrue(client.contains("ClientVisualScarManager.resetConnectionEpoch"));
		assertTrue(client.contains("ClientVisualScarRenderer.closeResources"));
		assertFalse(client.contains("ClientVisualScarManager.resetResources"));
		String renderer = Files.readString(Path.of(
				"src/client/java/com/powers/client/fx/ClientVisualScarRenderer.java"));
		assertTrue(renderer.contains("RenderPipelines.DEBUG_QUADS"));
		assertTrue(renderer.contains("VisualScarPresentation.profile"));
		assertTrue(renderer.contains("VisualScarMotifGeometry.mesh"));
		assertTrue(renderer.contains("VisualScarMotifGeometry.batchNearestCandidates"));
		assertFalse(renderer.contains("ResourceLocation"));
		assertFalse(renderer.contains("Shader"));
		String manager = Files.readString(Path.of(
				"src/client/java/com/powers/client/fx/ClientVisualScarManager.java"));
		assertTrue(manager.contains("VisualScarResyncPayload"));
		assertTrue(manager.contains("receiveObserved"));
		assertTrue(manager.contains("needsAuthoritativeResync"));
		assertTrue(manager.contains("localTick"));
	}

	@Test
	void directAndFaultDelayedDeliveryRecheckExactSessionImmediatelyBeforeFabricSend()
			throws IOException {
		String transport = source("network/PowersPlayNetworking.java");
		String compact = transport.replaceAll("\\s+", "");
		assertTrue(transport.contains("sendGuarded(ServerPlayer player"));
		assertTrue(transport.contains("Predicate<ServerPlayer> sessionPredicate"));
		assertTrue(transport.contains("Consumer<GuardedSendFailure> failureCallback"));
		assertTrue(compact.contains("if(sessionPredicate.test(player)"
				+ "&&ServerPlayNetworking.canSend(player,payload.type())){"
				+ "ServerPlayNetworking.send(player,payload);"));
		assertTrue(compact.contains("if(sessionPredicate.test(current)"
				+ "&&ServerPlayNetworking.canSend(current,value.type())){"
				+ "ServerPlayNetworking.send(current,value);"));
		String service = source("fx/VisualScarService.java");
		assertTrue(service.contains("PowersPlayNetworking.sendGuarded"));
		assertTrue(service.contains("GuardedSendFailure.UNSUPPORTED_CAPABILITY"));
		assertTrue(service.contains("cancelWithoutRetryOrResync"));
		assertTrue(service.contains("sessionCurrent") && service.contains("markNeedsResync"));
		assertTrue(service.contains("discardStaleFailure"));
		assertTrue(service.contains("onGuardedSendSuccess"));
		assertTrue(service.contains("RESET_DIMENSION"));
		assertTrue(service.contains("deliveryGeneration"));
		assertTrue(service.contains("beginSnapshotCreatesAfterResetSuccess"));
		assertFalse(service.contains("beginSnapshotCreatesBeforeResetSuccess"));

		String gameTests = Files.readString(Path.of(
				"src/gametest/java/com/powers/network/PacketFaultGameTests.java"));
		assertTrue(gameTests.contains("visualScarFaultDelayedSessionBoundary"));
		assertTrue(gameTests.contains("PacketFaultProfile") && gameTests.contains("delayTicks(6)"));
		assertTrue(gameTests.contains("changeDimension"));
		assertTrue(gameTests.contains("replaceConnection"));
		assertTrue(gameTests.contains("assertNoScarPayloadDeliveredToStaleSession"));
		String boundaryTests = Files.readString(Path.of(
				"src/gametest/java/com/powers/fx/VisualScarBoundaryGameTests.java"));
		assertTrue(boundaryTests.contains("unsupportedObserverIsCancelledWithoutRetryOrResync"));
		assertTrue(gameTests.contains("visualScarFalseSessionPredicateFailsAtProductionBoundary"));
		assertTrue(gameTests.contains("PowersPlayNetworking.sendGuarded"));
		assertTrue(gameTests.contains("visualScarFailureCallbackConvergesActiveClient"));
		for (String caseName : List.of("falsePredicate", "injectedLoss", "queueOverflow",
				"expiry", "loss1Percent", "loss5Percent")) assertTrue(gameTests.contains(caseName));
		assertTrue(gameTests.contains("assertActualActiveClientConverged"));
	}

	@Test
	void runtimeGalleryIncludesOpaqueWallOcclusionAcceptance() throws IOException {
		String clientTests = Files.readString(Path.of(
				"src/gametest/java/com/powers/gametest/PowersClientGameTests.java"));
		assertTrue(clientTests.contains("visualScarOccludedWall"));
		assertTrue(clientTests.contains("vfx004-scar-visible-front"));
		assertTrue(clientTests.contains("vfx004-scar-occluded-wall"));
		assertTrue(clientTests.contains("setXRot(35.0F)"));
		assertTrue(clientTests.contains("ClientVisualScarManager.entries()"));
		assertTrue(clientTests.contains("client.level.getBlockState(expectedSupport)"));
		assertTrue(clientTests.contains("client.level.getBlockState(expectedWall.above(y)).is(Blocks.STONE)"));
		assertTrue(clientTests.contains("context.waitTicks(60)"));
		assertTrue(clientTests.contains("context.waitTicks(5)"));
		assertTrue(clientTests.contains("assertOcclusionPipelineConfigured"));
		assertFalse(clientTests.contains("assertOccludedScarPixelsAbsent"));
		assertTrue(clientTests.contains("visualScarPresentationMatrix"));
		assertTrue(clientTests.contains("VisualScarService.request"));
		assertTrue(clientTests.contains("visualScarResourceReloadContinuity"));
		assertTrue(clientTests.contains("vfx004-scar-post-resource-reload"));
		assertTrue(clientTests.contains("client.reloadResourcePacks()"));
		assertTrue(clientTests.contains("Semantic scar changed across resource reload"));
		assertTrue(clientTests.contains("assertEquals(30, captureIds.size())"));
		assertTrue(clientTests.contains("vfx004-scar-matrix-"));
		assertTrue(clientTests.contains("renderActualMotifMesh"));
		assertTrue(clientTests.contains("assertMotifTopologyVisible"));
		assertTrue(clientTests.contains("assertNoKeyOrSwatchSubstitute"));
		assertTrue(clientTests.indexOf("quiesceVisuals(context);")
				< clientTests.indexOf("visualScarPresentationMatrix(context, singleplayer);"));
		assertTrue(clientTests.contains("ClientCelestialRuinFx.reset()"));
		String build = Files.readString(Path.of("build.gradle"));
		assertTrue(build.contains("vfx004ClientOnly"));
	}

	private static String source(String relative) throws IOException {
		return Files.readString(ROOT.resolve(relative));
	}
}

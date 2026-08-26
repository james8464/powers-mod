package com.powers.client;

import com.powers.client.fx.ClientVisualScarManager;
import com.powers.fx.ClientVisualScarState;
import com.powers.fx.VisualScarRules;
import com.powers.fx.VisualScarService;
import com.powers.testing.network.PacketFaultController;
import com.powers.testing.network.PacketFaultDirection;
import com.powers.testing.network.PacketFaultFamily;
import com.powers.testing.network.PacketFaultMetrics;
import com.powers.testing.network.PacketFaultProfile;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Real integrated-client acceptance for VFX-004 authoritative scar recovery.
 *
 * <p>Every scar enters through {@link VisualScarService#request}; its delivery therefore crosses
 * the production {@code PowersPlayNetworking.sendGuarded} edge and the configured
 * {@link PacketFaultController} before {@link ClientVisualScarManager} observes it.</p>
 */
public final class VisualScarFaultAcceptanceClientGameTests implements FabricClientGameTest {
	private static final int SCAR_COUNT = 64;
	private static final int MAX_LOSS_ROUNDS = 20;
	private static final long PROFILE_SEED = 0x5CA4_F001L;

	@Override
	public void runTest(ClientGameTestContext context) {
		context.restoreDefaultGameOptions();
		TestWorldSave worldSave;
		AtomicReference<ClientVisualScarState.HandlerStamp> originalConnection = new AtomicReference<>();
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			context.waitFor(client -> client.player != null && client.level != null);
			context.waitFor(client -> ClientVisualScarManager.entries().isEmpty());

			AtomicReference<List<BlockPos>> supports = new AtomicReference<>();
			AtomicReference<Map<Long, Integer>> expected = new AtomicReference<>(Map.of());
			prepareSupports(singleplayer, supports);
			publishRound(singleplayer, supports.get(), expected, 1);
			awaitAuthoritativeConvergence(context, singleplayer, expected, "initial");

			verifyProfile(context, singleplayer, supports.get(), expected, "delay150", 10);
			verifyProfile(context, singleplayer, supports.get(), expected, "delay300", 20);
			verifyProfile(context, singleplayer, supports.get(), expected, "duplicate", 30);
			verifyReorder(context, singleplayer, supports.get(), expected);
			verifyProfile(context, singleplayer, supports.get(), expected, "loss1", 50);
			verifyProfile(context, singleplayer, supports.get(), expected, "loss5", 100);
			verifySameServerSessionReplacement(context, singleplayer, supports.get(), expected);
			verifyDimensionBoundary(context, singleplayer, expected);
			context.runOnClient(client -> originalConnection.set(
					ClientVisualScarManager.captureHandlerStamp(client)));
			verifyExpiryRemoval(context, singleplayer, supports.get(), expected);
			verifyMovementIntoObservationRange(context, singleplayer, expected);
			worldSave = singleplayer.getWorldSave();
		} finally {
			context.runOnClient(client -> {
				if (client.level != null) ClientVisualScarManager.rendererResourcesRecreated();
			});
		}
		verifyIntegratedReconnect(context, worldSave, originalConnection.get());
	}

	private static void prepareSupports(TestSingleplayerContext singleplayer,
			AtomicReference<List<BlockPos>> supports) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			BlockPos origin = player.blockPosition();
			var positions = new java.util.ArrayList<BlockPos>(SCAR_COUNT);
			for (int z = 3; z < 11; z++) {
				for (int x = -4; x < 4; x++) {
					BlockPos support = new BlockPos(origin.getX() + x, origin.getY() - 1,
							origin.getZ() + z);
					player.level().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
					player.level().setBlockAndUpdate(support.above(), Blocks.AIR.defaultBlockState());
					positions.add(support.immutable());
				}
			}
			supports.set(List.copyOf(positions));
		});
	}

	private static void verifyProfile(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, List<BlockPos> supports,
			AtomicReference<Map<Long, Integer>> expected, String profile, int seedBase) {
		configure(singleplayer, profile);
		PacketFaultMetrics observed = PacketFaultMetrics.empty();
		int rounds = profile.startsWith("loss") ? MAX_LOSS_ROUNDS : 1;
		for (int round = 0; round < rounds; round++) {
			publishRound(singleplayer, supports, expected, seedBase + round);
			awaitAuthoritativeConvergence(context, singleplayer, expected, profile);
			observed = metrics(singleplayer);
			if (!profile.startsWith("loss") || observed.dropped() > 0L) break;
		}
		awaitFaultQueueEmpty(context, singleplayer, profile);
		observed = metrics(singleplayer);
		assertProfileWasReal(profile, observed);
		assertSupportsRemainAuthoritative(singleplayer, supports);
		clearFaults(singleplayer);
	}

	private static void verifyReorder(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, List<BlockPos> supports,
			AtomicReference<Map<Long, Integer>> expected) {
		configure(singleplayer, "reorder");
		publishRound(singleplayer, supports, expected, 40);
		context.waitTick();
		publishRound(singleplayer, supports, expected, 41);
		awaitAuthoritativeConvergence(context, singleplayer, expected, "reorder");
		awaitFaultQueueEmpty(context, singleplayer, "reorder");
		PacketFaultMetrics observed = metrics(singleplayer);
		assertProfileWasReal("reorder", observed);
		assertSupportsRemainAuthoritative(singleplayer, supports);
		clearFaults(singleplayer);
	}

	private static void verifySameServerSessionReplacement(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, List<BlockPos> supports,
			AtomicReference<Map<Long, Integer>> expected) {
		AtomicReference<MinecraftServer> retainedServer = new AtomicReference<>();
		AtomicReference<Long> originalSession = new AtomicReference<>();
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			retainedServer.set(server);
			originalSession.set(VisualScarService.diagnosticsForTest(
					server, player.getUUID()).sessionGeneration());
		});
		configure(singleplayer, "delay300");
		publishRound(singleplayer, supports, expected, 150);
		waitForQueuedScarDelivery(context, singleplayer);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			// Mirrors the production disconnect callback while the guarded send is delayed.
			VisualScarService.disconnect(player);
		});
		awaitAuthoritativeConvergence(context, singleplayer, expected, "session-predicate-boundary");
		awaitFaultQueueEmpty(context, singleplayer, "session-predicate-boundary");
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			long replacement = VisualScarService.diagnosticsForTest(
					server, player.getUUID()).sessionGeneration();
			if (server != retainedServer.get() || replacement <= originalSession.get()) {
				throw new AssertionError("Connection-session replacement did not retain the server and advance identity");
			}
		});
		PacketFaultMetrics observed = metrics(singleplayer);
		if (observed.cancelled() < 1L || observed.delayed() < SCAR_COUNT) {
			throw new AssertionError("Delayed stale session did not fail its real guarded predicate: "
					+ observed);
		}
		assertSupportsRemainAuthoritative(singleplayer, supports);
		clearFaults(singleplayer);
	}

	private static void waitForQueuedScarDelivery(ClientGameTestContext context,
			TestSingleplayerContext singleplayer) {
		for (int tick = 0; tick < 100; tick++) {
			AtomicReference<PacketFaultController.Diagnostics> diagnostics = new AtomicReference<>();
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				diagnostics.set(PacketFaultController.diagnostics(server, player));
			});
			if (diagnostics.get().metrics().offered() >= SCAR_COUNT
					&& diagnostics.get().queueDepth() > 0) return;
			context.waitTick();
		}
		throw new AssertionError("No delayed production scar delivery was queued");
	}

	private static void configure(TestSingleplayerContext singleplayer, String profile) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			PacketFaultProfile named = switch (profile) {
				case "loss1" -> PacketFaultProfile.named("loss1", PROFILE_SEED);
				case "loss5" -> PacketFaultProfile.named("loss5", PROFILE_SEED);
				default -> PacketFaultProfile.named(profile, PROFILE_SEED);
			};
			PacketFaultProfile configured = new PacketFaultProfile(named.id(), named.seed(),
					Set.of(PacketFaultDirection.CLIENTBOUND), Set.of(PacketFaultFamily.SCAR_FX),
					named.delayTicks(), named.lossPerTenThousand(), named.duplicatePerTenThousand(),
					named.reorderWindowTicks(), named.queueLimit(), named.lifetimeTicks(),
					named.workPerTick());
			PacketFaultController.configureScoped(server,
					configured, player);
		});
	}

	private static void publishRound(TestSingleplayerContext singleplayer, List<BlockPos> supports,
			AtomicReference<Map<Long, Integer>> expected, int round) {
		Map<Long, Integer> roundExpected = new LinkedHashMap<>();
		for (int index = 0; index < supports.size(); index++) {
			roundExpected.put(supports.get(index).asLong(), visualSeed(round, index));
		}
		expected.set(Map.copyOf(roundExpected));
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			for (int index = 0; index < supports.size(); index++) {
				boolean accepted = VisualScarService.request(player.level(), player,
						supports.get(index), Direction.UP,
						VisualScarRules.Impact.values()[index % VisualScarRules.Impact.values().length],
						visualSeed(round, index));
				if (!accepted) {
					throw new AssertionError("Production VisualScarService rejected scar " + index
							+ " in round " + round);
				}
			}
		});
	}

	private static void awaitAuthoritativeConvergence(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, AtomicReference<Map<Long, Integer>> expected,
			String profile) {
		for (int tick = 0; tick < 1_200; tick++) {
			AtomicBoolean matched = new AtomicBoolean();
			context.runOnClient(client -> matched.set(
					converged(ClientVisualScarManager.entries(), expected.get())));
			if (matched.get()) break;
			context.waitTick();
			if (tick == 1_199) {
				throw new AssertionError("Client scar state did not converge for " + profile
						+ ": entries=" + ClientVisualScarManager.entries().size()
						+ ", expected=" + expected.get().size() + ", metrics=" + metrics(singleplayer));
			}
		}
		context.runOnClient(client -> {
			if (!converged(ClientVisualScarManager.entries(), expected.get())) {
				throw new AssertionError("Client scar state diverged after convergence barrier");
			}
		});
	}

	private static boolean converged(List<ClientVisualScarState.Entry> entries,
			Map<Long, Integer> expected) {
		if (entries.size() != expected.size()) return false;
		for (ClientVisualScarState.Entry entry : entries) {
			Integer seed = expected.get(entry.position());
			if (seed == null || seed != entry.visualSeed() || entry.face() != Direction.UP.ordinal()
					|| entry.operation() != 0 || entry.leaseTicks() <= 0) return false;
		}
		return true;
	}

	private static void awaitFaultQueueEmpty(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, String profile) {
		for (int sample = 0; sample < 40; sample++) {
			AtomicReference<PacketFaultController.Diagnostics> diagnostics = new AtomicReference<>();
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				diagnostics.set(PacketFaultController.diagnostics(server, player));
			});
			if (diagnostics.get().queueDepth() == 0) return;
			System.out.println("VFX004_FAULT_DRAIN profile=" + profile + " sample=" + sample
					+ " " + diagnostics.get().line());
			for (int tick = 0; tick < 10; tick++) context.waitTick();
		}
		AtomicReference<PacketFaultController.Diagnostics> diagnostics = new AtomicReference<>();
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			diagnostics.set(PacketFaultController.diagnostics(server, player));
		});
		throw new AssertionError("Fault queue did not drain for " + profile + ": "
				+ diagnostics.get().line());
	}

	private static PacketFaultMetrics metrics(TestSingleplayerContext singleplayer) {
		AtomicReference<PacketFaultMetrics> result = new AtomicReference<>();
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			result.set(PacketFaultController.diagnostics(server, player).metrics());
		});
		return result.get();
	}

	private static void assertProfileWasReal(String profile, PacketFaultMetrics metrics) {
		if (metrics == null || metrics.offered() < SCAR_COUNT || metrics.delivered() < 1L) {
			throw new AssertionError(profile + " did not traverse the real faulted transport: " + metrics);
		}
		if (profile.startsWith("loss") && metrics.dropped() < 1L) {
			throw new AssertionError(profile + " injected no deterministic real-path loss: " + metrics);
		}
		if (profile.startsWith("delay") && metrics.delayed() < 1L) {
			throw new AssertionError(profile + " injected no real-path delay: " + metrics);
		}
		if ("duplicate".equals(profile) && metrics.duplicated() < 1L) {
			throw new AssertionError("duplicate injected no real-path duplicate: " + metrics);
		}
		if ("reorder".equals(profile) && (metrics.delayed() < 1L || metrics.reordered() < 1L)) {
			throw new AssertionError("reorder did not suppress a stale real-path frame: " + metrics);
		}
		if (metrics.duplicateSideEffects() != 0L || metrics.overflowed() != 0L
				|| metrics.expired() != 0L) {
			throw new AssertionError(profile + " violated bounded/idempotent delivery: " + metrics);
		}
	}

	private static void assertSupportsRemainAuthoritative(TestSingleplayerContext singleplayer,
			List<BlockPos> supports) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			for (BlockPos support : supports) {
				if (!player.level().getBlockState(support).is(Blocks.STONE)
						|| !player.level().getBlockState(support.above()).isAir()) {
					throw new AssertionError("Active authoritative scar support changed at " + support);
				}
			}
		});
	}

	private static void clearFaults(TestSingleplayerContext singleplayer) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			PacketFaultController.clearScoped(server, player);
		});
	}

	private static void verifyDimensionBoundary(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, AtomicReference<Map<Long, Integer>> expected) {
		AtomicReference<Vec3> overworldPosition = new AtomicReference<>();
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			overworldPosition.set(player.position());
			var nether = server.getLevel(Level.NETHER);
			if (nether == null) throw new AssertionError("Integrated server has no Nether dimension");
			if (!(player.teleport(new TeleportTransition(nether, new Vec3(0.5, 80.0, 0.5), Vec3.ZERO,
					player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING)) instanceof ServerPlayer)) {
				throw new AssertionError("Real dimension transition to Nether failed");
			}
		});
		context.waitFor(client -> client.level != null && client.level.dimension().equals(Level.NETHER));
		context.waitFor(client -> ClientVisualScarManager.entries().isEmpty());

		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			var overworld = server.getLevel(Level.OVERWORLD);
			if (overworld == null) throw new AssertionError("Integrated server lost the Overworld");
			Vec3 returnTo = overworldPosition.get();
			if (!(player.teleport(new TeleportTransition(overworld, returnTo, Vec3.ZERO,
					player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING)) instanceof ServerPlayer)) {
				throw new AssertionError("Real dimension transition back to Overworld failed");
			}
		});
		context.waitFor(client -> client.level != null && client.level.dimension().equals(Level.OVERWORLD));
		awaitAuthoritativeConvergence(context, singleplayer, expected, "dimension-return");
	}

	private static void verifyExpiryRemoval(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, List<BlockPos> supports,
			AtomicReference<Map<Long, Integer>> expected) {
		publishRound(singleplayer, supports, expected, 200);
		awaitAuthoritativeConvergence(context, singleplayer, expected, "expiry-baseline");
		AtomicReference<Integer> observedLease = new AtomicReference<>();
		AtomicReference<Long> removeBaseline = new AtomicReference<>();
		context.runOnClient(client -> observedLease.set(ClientVisualScarManager.entries().stream()
				.mapToInt(ClientVisualScarState.Entry::leaseTicks).min().orElseThrow()));
		context.runOnClient(client -> removeBaseline.set(
				ClientVisualScarManager.removeDiagnostics().receipts()));
		context.waitTicks(Math.max(1, observedLease.get() - 25));
		context.runOnClient(client -> {
			if (!converged(ClientVisualScarManager.entries(), expected.get())) {
				throw new AssertionError("Scar expired before its advertised authoritative lease");
			}
		});
		for (int tick = 0; tick < 100; tick++) {
			AtomicBoolean authoritativeRemove = new AtomicBoolean();
			AtomicBoolean serverEmpty = new AtomicBoolean();
			context.runOnClient(client -> authoritativeRemove.set(
					ClientVisualScarManager.removeDiagnostics().receipts()
							- removeBaseline.get() == supports.size()));
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				serverEmpty.set(VisualScarService.diagnosticsForTest(
						server, player.getUUID()).activeCount() == 0);
			});
			if (authoritativeRemove.get() && serverEmpty.get()) break;
			context.waitTick();
			if (tick == 99) throw new AssertionError(
					"Authoritative exact-generation REMOVE did not cross production delivery");
		}
		context.runOnClient(client -> {
			if (!ClientVisualScarManager.entries().isEmpty()) {
				throw new AssertionError("Applied authoritative REMOVE retained client scars");
			}
		});
		assertSupportsRemainAuthoritative(singleplayer, supports);
		System.out.println("VFX004_EXPIRY_REMOVE count=" + supports.size()
				+ " advertisedLease=" + observedLease.get()
				+ " supportMutation=false clientEntries=0");
	}

	private static void verifyMovementIntoObservationRange(ClientGameTestContext context,
			TestSingleplayerContext singleplayer,
			AtomicReference<Map<Long, Integer>> expected) {
		AtomicReference<Vec3> destination = new AtomicReference<>();
		AtomicReference<ChunkPos> forcedChunk = new AtomicReference<>();
		AtomicReference<Double> initialDistance = new AtomicReference<>();
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			int remoteChunkX = Math.floorDiv(player.blockPosition().getX() + 320, 16);
			int remoteChunkZ = Math.floorDiv(player.blockPosition().getZ(), 16);
			BlockPos anchor = new BlockPos(remoteChunkX * 16 + 4,
					player.blockPosition().getY(), remoteChunkZ * 16 + 4);
			ChunkPos remoteChunk = new ChunkPos(remoteChunkX, remoteChunkZ);
			forcedChunk.set(remoteChunk);
			player.level().setChunkForced(remoteChunk.x(), remoteChunk.z(), true);
			double horizontalDistance = Math.hypot(anchor.getX() + 0.5 - player.getX(),
					anchor.getZ() + 0.5 - player.getZ());
			if (horizontalDistance <= 256.0) {
				throw new AssertionError("Remote scar fixture was inside observation range: "
						+ horizontalDistance);
			}
			initialDistance.set(horizontalDistance);
			Map<Long, Integer> remoteExpected = new LinkedHashMap<>();
			for (int index = 0; index < 8; index++) {
				BlockPos support = anchor.offset(index % 4, -1, index / 4);
				// The fixture loads the target area; VisualScarService itself still creates no ticket.
				player.level().getChunkAt(support);
				player.level().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
				player.level().setBlockAndUpdate(support.above(), Blocks.AIR.defaultBlockState());
				int seed = visualSeed(400, index);
				remoteExpected.put(support.asLong(), seed);
				if (!VisualScarService.request(player.level(), player, support, Direction.UP,
						VisualScarRules.Impact.values()[index % VisualScarRules.Impact.values().length], seed)) {
					throw new AssertionError("Remote production scar request was rejected: " + index);
				}
			}
			expected.set(Map.copyOf(remoteExpected));
			destination.set(new Vec3(anchor.getX() + 1.5, player.getY(), anchor.getZ() + 0.5));
		});
		context.waitTicks(20);
		context.runOnClient(client -> {
			if (!ClientVisualScarManager.entries().isEmpty()) {
				throw new AssertionError("Out-of-range client received remote active scars");
			}
		});
		System.out.println("VFX004_RANGE_BEFORE_ENTRY distance=" + initialDistance.get()
				+ " clientEntries=0 activeExpected="
				+ expected.get().size());

		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			Vec3 target = destination.get();
			player.teleportTo(target.x, target.y, target.z);
		});
		context.waitFor(client -> client.player != null
				&& client.player.position().distanceToSqr(destination.get()) < 4.0);
		awaitAuthoritativeConvergence(context, singleplayer, expected, "movement-enter-range");
		singleplayer.getServer().runOnServer(server -> {
			ChunkPos remoteChunk = forcedChunk.get();
			server.overworld().setChunkForced(remoteChunk.x(), remoteChunk.z(), false);
		});
		System.out.println("VFX004_RANGE_AFTER_ENTRY clientEntries=" + expected.get().size()
				+ " authoritativeConvergence=true");
	}

	private static void verifyIntegratedReconnect(ClientGameTestContext context,
			TestWorldSave worldSave, ClientVisualScarState.HandlerStamp originalConnection) {
		context.waitFor(client -> client.level == null && client.player == null);
		try (TestSingleplayerContext reconnected = worldSave.open()) {
			context.waitFor(client -> client.player != null && client.level != null);
			context.waitFor(client -> ClientVisualScarManager.entries().isEmpty());
			context.runOnClient(client -> {
				ClientVisualScarState.HandlerStamp current =
						ClientVisualScarManager.captureHandlerStamp(client);
				if (originalConnection == null
						|| current.connectionEpoch() <= originalConnection.connectionEpoch()) {
					throw new AssertionError("Integrated reconnect did not advance client connection epoch");
				}
			});
			AtomicReference<List<BlockPos>> supports = new AtomicReference<>();
			AtomicReference<Map<Long, Integer>> expected = new AtomicReference<>(Map.of());
			prepareSupports(reconnected, supports);
			publishRound(reconnected, supports.get(), expected, 300);
			awaitAuthoritativeConvergence(context, reconnected, expected, "integrated-reconnect");
			assertSupportsRemainAuthoritative(reconnected, supports.get());
		}
	}

	private static int visualSeed(int round, int index) {
		return 0x4000_0000 ^ round * 65_537 ^ index * 8_191;
	}
}

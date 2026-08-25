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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
			verifyDimensionBoundary(context, singleplayer, expected);
		} finally {
			context.runOnClient(client -> {
				if (client.level != null) ClientVisualScarManager.rendererResourcesRecreated();
			});
		}
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

	private static int visualSeed(int round, int index) {
		return 0x4000_0000 ^ round * 65_537 ^ index * 8_191;
	}
}

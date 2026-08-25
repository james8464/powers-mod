package com.powers.fx;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/** Actual integrated-server restart proof for the exhausted scar generation allocator. */
public final class VisualScarRestartClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		AtomicReference<MinecraftServer> exhaustedServer = new AtomicReference<>();
		try (TestSingleplayerContext first = context.worldBuilder().create()) {
			context.waitFor(client -> client.player != null && client.level != null);
			first.getServer().runOnServer(server -> {
				exhaustedServer.set(server);
				ServerPlayer owner = server.getPlayerList().getPlayers().getFirst();
				BlockPos support = prepare(owner, 0);
				VisualScarService.setGenerationForTest(server, Long.MAX_VALUE - 1);
				assertAccepted(owner, support, 1);
			});
			awaitGeneration(context, first, Long.MAX_VALUE, false);
			first.getServer().runOnServer(server -> {
				ServerPlayer owner = server.getPlayerList().getPlayers().getFirst();
				assertAccepted(owner, prepare(owner, 2), 2);
			});
			awaitGeneration(context, first, Long.MAX_VALUE, true);
			first.getServer().runOnServer(server -> {
				ServerPlayer owner = server.getPlayerList().getPlayers().getFirst();
				if (VisualScarService.request(owner.level(), owner, prepare(owner, 4), Direction.UP,
						VisualScarRules.Impact.FIRE, 3)) {
					throw new AssertionError("Exhausted allocator accepted a new request");
				}
			});
		}

		try (TestSingleplayerContext restarted = context.worldBuilder().create()) {
			context.waitFor(client -> client.player != null && client.level != null);
			restarted.getServer().runOnServer(server -> {
				if (server == exhaustedServer.get()) {
					throw new AssertionError("Fixture reused the exhausted integrated server");
				}
				ServerPlayer owner = server.getPlayerList().getPlayers().getFirst();
				assertAccepted(owner, prepare(owner, 0), 4);
			});
			awaitGeneration(context, restarted, 1, false);
		}
	}

	private static BlockPos prepare(ServerPlayer owner, int offset) {
		BlockPos support = owner.blockPosition().offset(offset, -1, 3);
		owner.level().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
		owner.level().setBlockAndUpdate(support.above(), Blocks.AIR.defaultBlockState());
		return support.immutable();
	}

	private static void assertAccepted(ServerPlayer owner, BlockPos support, int seed) {
		if (!VisualScarService.request(owner.level(), owner, support, Direction.UP,
				VisualScarRules.Impact.FIRE, seed)) {
			throw new AssertionError("Production service rejected generation fixture request");
		}
	}

	private static void awaitGeneration(ClientGameTestContext context,
			TestSingleplayerContext singleplayer, long generation, boolean disabled) {
		for (int tick = 0; tick < 40; tick++) {
			AtomicBoolean matched = new AtomicBoolean();
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				VisualScarService.TestDiagnostics diagnostics =
						VisualScarService.diagnosticsForTest(server, player.getUUID());
				matched.set(diagnostics.generation() == generation
						&& diagnostics.admissionsDisabled() == disabled);
			});
			if (matched.get()) return;
			context.waitTick();
		}
		throw new AssertionError("Allocator did not reach generation=" + generation
				+ " disabled=" + disabled);
	}
}

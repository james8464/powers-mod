package com.powers.fx;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Production-service acceptance for bounded request and delivery overflow. */
@SuppressWarnings("removal")
public final class VisualScarBoundaryGameTests {
	@GameTest(maxTicks = 20)
	public void requestQueueRejectsThe129thRequestForOneOwner(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		VisualScarService.clear(server);
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
		for (int index = 0; index < 128; index++) {
			helper.assertTrue(VisualScarService.request(helper.getLevel(), owner,
					base.offset(index, 0, 0), Direction.UP, VisualScarRules.Impact.BEAM, index),
					"Production request queue rejected in-cap request " + index);
		}
		helper.assertFalse(VisualScarService.request(helper.getLevel(), owner,
				base.offset(128, 0, 0), Direction.UP, VisualScarRules.Impact.BEAM, 128),
				"Production request queue accepted request 129 for one owner");
		VisualScarService.clear(server);
		helper.succeed();
	}

	@GameTest(maxTicks = 20)
	public void deliveryQueueCapsAt2048AndMarksProductionResync(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		VisualScarService.clear(server);
		ServerPlayer observer = helper.makeMockServerPlayerInLevel();
		VisualScarService.observeSessionForTest(server, observer.getUUID());
		String dimension = observer.level().dimension().identifier().toString();
		for (int index = 0; index < 2_049; index++) {
			BlockPos support = observer.blockPosition().offset(index % 46, 0, index / 46);
			VisualScarService.broadcastForTest(server, dimension, new ScarFxProtocolRules.Wire(
					ScarFxProtocolRules.CREATE_OR_UPDATE, support.asLong(), Direction.UP.ordinal(),
					VisualScarRules.Impact.BEAM.ordinal(), VisualScarRules.Material.STONE.ordinal(),
					index, 1, 40));
		}
		VisualScarService.TestDiagnostics diagnostics =
				VisualScarService.diagnosticsForTest(server, observer.getUUID());
		helper.assertTrue(diagnostics.pendingObserver() == 2_048,
				"Observer delivery queue exceeded its 2,048 cap: " + diagnostics);
		helper.assertTrue(diagnostics.pendingGlobal() == 2_048,
				"Global delivery accounting diverged after observer overflow: " + diagnostics);
		helper.assertTrue(diagnostics.needsResync(),
				"Delivery overflow did not mark bounded authoritative resync");
		VisualScarService.clear(server);
		helper.succeed();
	}
}

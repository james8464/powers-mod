package com.powers.client;

import com.powers.network.SuperSpeedComfortPackets;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Real-client proof for the injected owner-local reduced-motion FOV boundary. */
public final class SuperSpeedComfortClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.restoreDefaultGameOptions();
		try (var singleplayer = context.worldBuilder().create()) {
			context.waitFor(client -> client.player != null && client.level != null);
			context.runOnClient(client -> {
				client.options.fov().set(70);
				client.options.fovEffectScale().set(1.0);
				client.options.screenEffectScale().set(1.0);
				client.player.setSprinting(true);
				ClientSuperSpeedComfort.handle(new SuperSpeedComfortPackets.Payload(true, 200));
			});
			context.waitFor(client -> client.gameRenderer.mainCamera().getFov() > 70.5F);
			context.runOnClient(client -> {
				client.options.screenEffectScale().set(0.0);
				ClientSuperSpeedComfort.handle(new SuperSpeedComfortPackets.Payload(true, 200));
			});
			context.waitFor(client -> Math.abs(client.gameRenderer.mainCamera().getFov() - 70.0F) < 0.25F);
			context.runOnClient(client -> {
				ClientSuperSpeedComfort.clear();
				client.player.setSprinting(false);
				client.options.screenEffectScale().set(1.0);
			});
		}
	}
}

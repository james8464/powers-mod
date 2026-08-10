package com.powers;

import com.powers.player.PlayerTickCadence;
import com.powers.power.PowerAbilityRuntime;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Executes every POWERS per-player concern in one predictable linear pass. */
final class PlayerTickCoordinator {
	private PlayerTickCoordinator() {
	}

	static void tick(MinecraftServer server, int tick) {
		PlayerTickCadence cadence = PlayerTickCadence.at(tick);
		if (cadence.fiveTick()) PowerAbilityRuntime.tickFrequent(server);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			PowersMod.tickPlayer(player, tick, cadence);
		}
	}
}

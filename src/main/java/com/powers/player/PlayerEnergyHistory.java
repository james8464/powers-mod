package com.powers.player;

import net.minecraft.server.level.ServerPlayer;

/** Runtime access to the session-scoped authoritative energy ledger. */
public final class PlayerEnergyHistory {
	private static final EnergyHistoryLedger LEDGER = new EnergyHistoryLedger();

	private PlayerEnergyHistory() {
	}

	static void record(ServerPlayer player, EnergyHistorySource source, int before, int after) {
		LEDGER.record(player.getUUID(), player.level().getGameTime(), source, before, after);
	}

	public static EnergyHistorySnapshot snapshot(ServerPlayer player) {
		return LEDGER.snapshot(player.getUUID());
	}

	public static void forget(ServerPlayer player) {
		LEDGER.forget(player.getUUID());
	}

	public static void clear() {
		LEDGER.clear();
	}
}

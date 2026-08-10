package com.powers.network;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Hard fixed-window budgets for every client-triggered gameplay lane. */
public final class PacketRateLimiter {
	private static final long WINDOW_TICKS = 20L;
	private static final PacketRateLimiter GLOBAL = new PacketRateLimiter();

	/** Independent limits prevent cheap menu traffic from starving combat input. */
	public enum Lane {
		ACTIVATION(20), SELECTION(12), TRAVEL(4), RANK(8), ARTIFACT(20), LOCATOR(2), RITUAL(8),
		COMPANION(4);

		private final int limit;

		Lane(int limit) {
			this.limit = limit;
		}

		public int limit() {
			return limit;
		}
	}

	private record Key(UUID player, Lane lane) {
	}

	private static final class Window {
		private long startedAt;
		private int accepted;

		private Window(long startedAt) {
			this.startedAt = startedAt;
		}
	}

	private final Map<Key, Window> windows = new HashMap<>();

	/** Returns whether one request fits the player's current lane budget. */
	public boolean allow(UUID player, Lane lane, long currentTick) {
		if (player == null || lane == null || currentTick < 0L) return false;
		Key key = new Key(player, lane);
		Window window = windows.computeIfAbsent(key, ignored -> new Window(currentTick));
		if (currentTick < window.startedAt || currentTick - window.startedAt >= WINDOW_TICKS) {
			window.startedAt = currentTick;
			window.accepted = 0;
		}
		if (window.accepted >= lane.limit()) return false;
		window.accepted++;
		return true;
	}

	/** Applies the global server budget using the authoritative server clock. */
	public static boolean allow(ServerPlayer player, Lane lane) {
		return player != null && GLOBAL.allow(player.getUUID(), lane,
				player.level().getServer().getTickCount());
	}

	public void forget(UUID player) {
		windows.keySet().removeIf(key -> key.player().equals(player));
	}

	public static void forgetPlayer(UUID player) {
		GLOBAL.forget(player);
	}

	public static void clearGlobal() {
		GLOBAL.windows.clear();
	}
}

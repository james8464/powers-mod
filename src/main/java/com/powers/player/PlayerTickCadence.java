package com.powers.player;

/** Pure cadence flags used to keep all per-player server work inside one pass. */
public record PlayerTickCadence(boolean fiveTick, boolean second, boolean passiveRefresh) {
	public static PlayerTickCadence at(int tick) {
		return new PlayerTickCadence(tick % 5 == 0, tick % 20 == 0, tick % 100 == 0);
	}

	/** Linear visit contract used by deterministic multiplayer regression tests. */
	public static int playerVisits(int onlinePlayers) {
		return Math.max(0, onlinePlayers);
	}
}

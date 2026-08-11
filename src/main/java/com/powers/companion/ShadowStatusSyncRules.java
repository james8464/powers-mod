package com.powers.companion;

/** Pure change detection and heartbeat cadence for owner Shadow snapshots. */
final class ShadowStatusSyncRules {
	record Snapshot(boolean active, int energy, String stance, boolean revealed,
			boolean suppressed, int recallTicks) {
	}

	private static final int HEARTBEAT_TICKS = 20;

	private ShadowStatusSyncRules() {
	}

	static boolean shouldSend(Snapshot previous, Snapshot current, long serverTick,
			boolean immediate) {
		if (immediate || previous == null) return true;
		boolean contextChanged = previous.active() != current.active()
				|| !previous.stance().equals(current.stance())
				|| previous.revealed() != current.revealed()
				|| previous.suppressed() != current.suppressed()
				|| (previous.recallTicks() > 0) != (current.recallTicks() > 0);
		return contextChanged || Math.floorMod(serverTick, HEARTBEAT_TICKS) == 0;
	}
}

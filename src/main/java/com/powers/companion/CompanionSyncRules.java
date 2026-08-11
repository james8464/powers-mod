package com.powers.companion;

/** Pure bounds used to distribute globally revealed Shadows across server ticks. */
final class CompanionSyncRules {
	static final int UPDATE_INTERVAL_TICKS = 5;
	private static final int MAX_PACKETS_PER_TICK = 4_096;
	private static final int MIN_VIEWERS_PER_UPDATE = 8;
	private static final int MAX_VIEWERS_PER_UPDATE = 128;

	private CompanionSyncRules() {
	}

	static boolean shouldUpdate(int serverTick, long sessionId) {
		return Math.floorMod(serverTick, UPDATE_INTERVAL_TICKS)
				== Math.floorMod(sessionId, UPDATE_INTERVAL_TICKS);
	}

	static int viewerAllowance(int activeSessions) {
		int dueSessions = Math.max(1,
				(activeSessions + UPDATE_INTERVAL_TICKS - 1) / UPDATE_INTERVAL_TICKS);
		return Math.clamp(MAX_PACKETS_PER_TICK / dueSessions,
				MIN_VIEWERS_PER_UPDATE, MAX_VIEWERS_PER_UPDATE);
	}

	static int rotatingIndex(long cursor, int size) {
		if (size <= 0) return 0;
		return (int) Math.floorMod(cursor, (long) size);
	}
}

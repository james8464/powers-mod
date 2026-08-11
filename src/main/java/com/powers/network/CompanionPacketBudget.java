package com.powers.network;

/** Small state holder enforcing a hard server-tick packet ceiling. */
final class CompanionPacketBudget {
	private final int limit;
	private long tick = Long.MIN_VALUE;
	private int used;

	CompanionPacketBudget(int limit) {
		if (limit < 1) throw new IllegalArgumentException("limit must be positive");
		this.limit = limit;
	}

	boolean claim(long currentTick) {
		if (currentTick != tick) {
			tick = currentTick;
			used = 0;
		}
		if (used >= limit) return false;
		used++;
		return true;
	}
}

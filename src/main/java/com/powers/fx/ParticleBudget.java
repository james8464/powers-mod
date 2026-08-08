package com.powers.fx;

/** Small per-world limiter that resets on a new game tick. */
public final class ParticleBudget {
	private final int limit;
	private long tick = Long.MIN_VALUE;
	private int used;

	public ParticleBudget(int limit) {
		this.limit = Math.max(1, limit);
	}

	public int claim(long currentTick, int requested) {
		if (requested <= 0) return 0;
		if (tick != currentTick) {
			tick = currentTick;
			used = 0;
		}
		int granted = Math.min(requested, Math.max(0, limit - used));
		used += granted;
		return granted;
	}

	int limit() {
		return limit;
	}
}

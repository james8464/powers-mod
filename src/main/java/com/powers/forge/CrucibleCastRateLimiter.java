package com.powers.forge;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Exact same-tick replay guard independent of cooldowns. */
public final class CrucibleCastRateLimiter {
	private final Map<UUID, Integer> lastTick = new HashMap<>();

	public boolean allow(UUID playerId, int tick) {
		Integer previous = lastTick.put(playerId, tick);
		return previous == null || previous != tick;
	}

	public void forget(UUID playerId) {
		lastTick.remove(playerId);
	}

	public void clear() {
		lastTick.clear();
	}
}

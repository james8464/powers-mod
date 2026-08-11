package com.powers.protection;

import java.util.LinkedHashMap;
import java.util.UUID;

/** Fixed-capacity, tick-window suppression for repeated consent-denial audit events. */
public final class ConsentAuditRateLimiter {
	private record Key(UUID caster, UUID target, ConsentKind kind,
			ConsentOverrideRules.Decision decision) { }

	private final int capacity;
	private final long intervalTicks;
	private final LinkedHashMap<Key, Long> lastLogged = new LinkedHashMap<>();

	public ConsentAuditRateLimiter(int capacity, long intervalTicks) {
		this.capacity = Math.max(1, capacity);
		this.intervalTicks = Math.max(1L, intervalTicks);
	}

	public synchronized boolean shouldLog(long tick, UUID caster, UUID target, ConsentKind kind,
			ConsentOverrideRules.Decision decision) {
		Key key = new Key(caster, target, kind, decision);
		Long previous = lastLogged.get(key);
		if (previous != null && tick >= previous && tick - previous < intervalTicks) return false;
		if (previous == null && lastLogged.size() >= capacity) {
			var iterator = lastLogged.entrySet().iterator();
			iterator.next();
			iterator.remove();
		}
		lastLogged.put(key, tick);
		return true;
	}

	public synchronized int size() {
		return lastLogged.size();
	}

	public synchronized void clear() {
		lastLogged.clear();
	}
}

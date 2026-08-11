package com.powers.power.travel;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Exact-once ownership for five-second teleport storms. */
public final class TeleportStormTracker {
	private final Set<UUID> active = new HashSet<>();

	public synchronized boolean begin(UUID owner) {
		return owner != null && active.add(owner);
	}

	public synchronized boolean finish(UUID owner) {
		return owner != null && active.remove(owner);
	}

	public synchronized int activeCount() {
		return active.size();
	}

	public synchronized void clear() {
		active.clear();
	}
}

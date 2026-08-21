package com.powers.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Monotonic per-session input guard for the server-owned remote vessel. */
public final class VesselControlSequence {
	private final Map<UUID, Long> latest = new HashMap<>();

	public boolean accept(UUID owner, long sequence) {
		if (owner == null || sequence < 0L) return false;
		Long previous = latest.get(owner);
		if (previous != null && sequence <= previous) return false;
		latest.put(owner, sequence);
		return true;
	}

	public void clear(UUID owner) {
		latest.remove(owner);
	}

	public void clearAll() {
		latest.clear();
	}
}

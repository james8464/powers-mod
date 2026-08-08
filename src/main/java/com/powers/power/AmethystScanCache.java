package com.powers.power;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Caches only the expensive nearby-block scan; inventory and powered wards stay immediate. */
public final class AmethystScanCache {
	private record Entry(String dimension, int x, int y, int z, long expiresAt, boolean result) {
	}

	private final Map<UUID, Entry> entries = new HashMap<>();

	public Boolean get(UUID player, String dimension, int x, int y, int z, long now) {
		Entry entry = entries.get(player);
		if (entry == null || entry.expiresAt() < now || entry.x() != x || entry.y() != y || entry.z() != z
				|| !entry.dimension().equals(dimension)) return null;
		return entry.result();
	}

	public void put(UUID player, String dimension, int x, int y, int z, long expiresAt, boolean result) {
		entries.put(player, new Entry(dimension, x, y, z, expiresAt, result));
	}

	public void remove(UUID player) {
		entries.remove(player);
	}

	public void clear() {
		entries.clear();
	}
}

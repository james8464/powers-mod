package com.powers.network;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Per-tick visual-only deduplication by observer, dimension, chunk, and action. */
public final class FxPacketCoalescer {
	private record Key(UUID observer, String dimension, int chunkX, int chunkZ, String action) { }

	private final int capacity;
	private final Set<Key> seen = new HashSet<>();
	private long tick = Long.MIN_VALUE;

	public FxPacketCoalescer(int capacity) {
		if (capacity < 1) throw new IllegalArgumentException("Capacity must be positive");
		this.capacity = capacity;
	}

	/** Returns false only for an exact repeated visual update in the current tick. */
	public boolean allow(long currentTick, UUID observer, String dimension,
			int chunkX, int chunkZ, String action) {
		if (currentTick != tick) {
			tick = currentTick;
			seen.clear();
		}
		Key key = new Key(Objects.requireNonNull(observer), Objects.requireNonNull(dimension),
				chunkX, chunkZ, Objects.requireNonNull(action));
		if (seen.contains(key)) return false;
		if (seen.size() >= capacity) return true;
		seen.add(key);
		return true;
	}

	public void clear() {
		seen.clear();
		tick = Long.MIN_VALUE;
	}
}

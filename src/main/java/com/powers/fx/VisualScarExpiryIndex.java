package com.powers.fx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/** Indexes one absolute expiry per key with bounded earliest-first removal and no future inspection. */
public final class VisualScarExpiryIndex<K> {
	private final int capacity;
	private final Map<K, Long> expiryByKey = new HashMap<>();
	private final NavigableMap<Long, LinkedHashSet<K>> keysByExpiry = new TreeMap<>();

	/** Creates an empty index whose capacity cannot exceed the global active-scar ceiling. */
	public VisualScarExpiryIndex(int capacity) {
		if (capacity < 1 || capacity > 2_048) throw new IllegalArgumentException("invalid expiry capacity");
		this.capacity = capacity;
	}

	/** Inserts or updates one key while preserving exact single-membership accounting. */
	public boolean put(K key, long expiresAt) {
		Objects.requireNonNull(key, "key");
		if (expiresAt < 0 || !expiryByKey.containsKey(key) && expiryByKey.size() >= capacity) return false;
		Long previous = expiryByKey.put(key, expiresAt);
		if (previous != null) removeFromBucket(key, previous);
		keysByExpiry.computeIfAbsent(expiresAt, ignored -> new LinkedHashSet<>()).add(key);
		return true;
	}

	/** Removes one exact key from both lookup and ordered expiry ownership. */
	public boolean remove(K key) {
		Long expiry = expiryByKey.remove(key);
		if (expiry == null) return false;
		removeFromBucket(key, expiry);
		return true;
	}

	/** Polls at most the requested due keys without visiting any future expiry bucket. */
	public Due<K> pollDue(long now, int maximum) {
		if (now < 0 || maximum < 0 || maximum > 64) {
			throw new IllegalArgumentException("invalid expiry work bound");
		}
		List<K> due = new ArrayList<>(maximum);
		while (due.size() < maximum && !keysByExpiry.isEmpty()) {
			var first = keysByExpiry.firstEntry();
			if (first.getKey() > now) break;
			var iterator = first.getValue().iterator();
			while (iterator.hasNext() && due.size() < maximum) {
				K key = iterator.next();
				iterator.remove();
				expiryByKey.remove(key);
				due.add(key);
			}
			if (first.getValue().isEmpty()) keysByExpiry.pollFirstEntry();
		}
		return new Due<>(due, due.size());
	}

	/** Returns the exact indexed membership count. */
	public int size() {
		return expiryByKey.size();
	}

	/** Reports whether one key remains indexed. */
	public boolean contains(K key) {
		return expiryByKey.containsKey(key);
	}

	private void removeFromBucket(K key, long expiry) {
		LinkedHashSet<K> bucket = keysByExpiry.get(expiry);
		if (bucket == null) return;
		bucket.remove(key);
		if (bucket.isEmpty()) keysByExpiry.remove(expiry);
	}

	public record Due<K>(List<K> keys, int inspected) {
		public Due {
			keys = List.copyOf(keys);
		}
	}
}

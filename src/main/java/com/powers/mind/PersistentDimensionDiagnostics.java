package com.powers.mind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded operator evidence for persistent records that name a dimension no
 * longer present in the active registry. Values are identifiers only: player
 * names, coordinates, and other sensitive state never enter this index.
 */
public final class PersistentDimensionDiagnostics {
	private static final int MAX_DISTINCT = 128;
	private static final Map<Key, Integer> ISSUES = new LinkedHashMap<>();
	private static long droppedDistinctKeys;

	/** A coalesced, non-sensitive recovery issue. */
	public record Issue(String feature, String dimension, int occurrences) {
	}

	/** Immutable diagnostic view. */
	public record Snapshot(List<Issue> issues, long droppedDistinctKeys) {
		public Snapshot {
			issues = List.copyOf(issues);
		}
	}

	private record Key(String feature, String dimension) {
	}

	private PersistentDimensionDiagnostics() {
	}

	/** Records one failure while bounding memory and rejecting log-forging text. */
	public static synchronized void record(String feature, String dimension) {
		if (!safe(feature) || !safe(dimension)) return;
		Key key = new Key(feature, dimension);
		Integer existing = ISSUES.get(key);
		if (existing != null) {
			ISSUES.put(key, existing == Integer.MAX_VALUE ? existing : existing + 1);
			return;
		}
		if (ISSUES.size() >= MAX_DISTINCT) {
			droppedDistinctKeys++;
			return;
		}
		ISSUES.put(key, 1);
	}

	/** Returns a stable copy suitable for `/powers diagnose`. */
	public static synchronized Snapshot snapshot() {
		List<Issue> copy = new ArrayList<>(ISSUES.size());
		ISSUES.forEach((key, count) -> copy.add(new Issue(key.feature(), key.dimension(), count)));
		return new Snapshot(copy, droppedDistinctKeys);
	}

	/** Clears process-local counters during server shutdown and isolated tests. */
	public static synchronized void clear() {
		ISSUES.clear();
		droppedDistinctKeys = 0;
	}

	private static boolean safe(String value) {
		if (value == null || value.isBlank() || value.length() > 256) return false;
		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			if (!(character >= 'a' && character <= 'z')
					&& !(character >= 'A' && character <= 'Z')
					&& !(character >= '0' && character <= '9')
					&& character != '_' && character != '-' && character != '.' && character != ':') return false;
		}
		return true;
	}
}

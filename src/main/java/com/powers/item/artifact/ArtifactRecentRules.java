package com.powers.item.artifact;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Pure bounds and reconciliation for the per-alignment recent-action history. */
public final class ArtifactRecentRules {
	public static final int LIMIT = 8;

	private ArtifactRecentRules() {
	}

	/** Moves one canonical successful selection to the front of the bounded history. */
	public static List<String> record(List<String> recent, String canonicalKey) {
		if (!valid(canonicalKey)) return reconcile(recent, recent == null ? List.of() : recent);
		List<String> next = new ArrayList<>(LIMIT);
		next.add(canonicalKey);
		if (recent != null) {
			for (String key : recent) {
				if (next.size() == LIMIT) break;
				if (valid(key) && !next.contains(key)) next.add(key);
			}
		}
		return List.copyOf(next);
	}

	/** Keeps the first occurrence of each available canonical key in display order. */
	public static List<String> reconcile(List<String> recent, List<String> availableKeys) {
		if (recent == null || recent.isEmpty() || availableKeys == null || availableKeys.isEmpty()) {
			return List.of();
		}
		Set<String> available = new LinkedHashSet<>(availableKeys);
		List<String> result = new ArrayList<>(LIMIT);
		for (String key : recent) {
			if (result.size() == LIMIT) break;
			if (valid(key) && available.contains(key) && !result.contains(key)) result.add(key);
		}
		return List.copyOf(result);
	}

	private static boolean valid(String key) {
		return key != null && !key.isBlank() && key.length() <= 96;
	}
}

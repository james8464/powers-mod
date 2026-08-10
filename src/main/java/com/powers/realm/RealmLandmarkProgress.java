package com.powers.realm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Process-local per-site construction progress; world blocks remain authoritative. */
public final class RealmLandmarkProgress {
	private static final char SEPARATOR = '\u0000';
	private final Map<String, Set<String>> completed = new HashMap<>();

	public RealmLandmarkProgress() {
	}

	public RealmLandmarkProgress(List<String> snapshot) {
		if (snapshot == null) return;
		for (String encoded : snapshot) {
			if (encoded == null) continue;
			int split = encoded.indexOf(SEPARATOR);
			if (split <= 0 || split >= encoded.length() - 1) continue;
			complete(encoded.substring(0, split), encoded.substring(split + 1));
		}
	}

	public boolean complete(String dimension, String site) {
		return completed.computeIfAbsent(dimension, ignored -> new HashSet<>()).add(site);
	}

	public List<String> missing(String dimension, List<String> sites) {
		Set<String> known = completed.getOrDefault(dimension, Set.of());
		return sites.stream().filter(site -> !known.contains(site)).toList();
	}

	public int completedCount() {
		return completed.values().stream().mapToInt(Set::size).sum();
	}

	public List<String> snapshot() {
		return completed.entrySet().stream()
				.flatMap(entry -> entry.getValue().stream()
						.map(site -> entry.getKey() + SEPARATOR + site))
				.sorted().toList();
	}

	public void clear() {
		completed.clear();
	}
}

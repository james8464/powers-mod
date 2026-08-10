package com.powers.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Reverse-tracked normalized-name index with ambiguity-bounded reads. */
public final class UniqueNameIndex<T> {
	private final Map<String, Set<T>> byName = new HashMap<>();
	private final Map<T, String> byIdentity = new HashMap<>();

	public void upsert(T identity, String name) {
		remove(identity);
		String normalized = normalize(name);
		if (identity == null || normalized.isEmpty()) return;
		byIdentity.put(identity, normalized);
		byName.computeIfAbsent(normalized, ignored -> new LinkedHashSet<>()).add(identity);
	}

	public void remove(T identity) {
		String previous = byIdentity.remove(identity);
		if (previous == null) return;
		Set<T> identities = byName.get(previous);
		if (identities == null) return;
		identities.remove(identity);
		if (identities.isEmpty()) byName.remove(previous);
	}

	public List<T> candidates(String name, int limit) {
		Set<T> identities = byName.get(normalize(name));
		if (identities == null || limit <= 0) return List.of();
		List<T> result = new ArrayList<>(identities);
		result.sort(java.util.Comparator.comparing(String::valueOf));
		return List.copyOf(result.subList(0, Math.min(limit, result.size())));
	}

	public void clear() {
		byName.clear();
		byIdentity.clear();
	}

	private static String normalize(String name) {
		return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
	}
}

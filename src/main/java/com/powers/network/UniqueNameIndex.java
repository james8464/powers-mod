package com.powers.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reverse-tracked normalized-name index with ambiguity-bounded reads. */
public final class UniqueNameIndex<T> {
	private final Map<String, Set<T>> byName = new HashMap<>();
	private final Map<T, String> byIdentity = new HashMap<>();
	private long queries;
	private long candidates;
	private long misses;
	private long staleRemovals;

	public record Diagnostics(long queries, long candidates, long misses, long staleRemovals,
			int entries, int names, long estimatedBytes) {
	}

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
		queries++;
		Set<T> identities = byName.get(normalize(name));
		if (identities == null || limit <= 0) {
			misses++;
			return List.of();
		}
		List<T> result = new ArrayList<>(identities);
		result.sort(java.util.Comparator.comparing(String::valueOf));
		int returned = Math.min(limit, result.size());
		candidates += returned;
		if (returned == 0) misses++;
		return List.copyOf(result.subList(0, returned));
	}

	public void removeStale(T identity) {
		int before = byIdentity.size();
		remove(identity);
		if (byIdentity.size() < before) staleRemovals++;
	}

	public Diagnostics diagnostics() {
		long characters = byName.keySet().stream().mapToLong(String::length).sum();
		long estimatedBytes = byIdentity.size() * 80L + byName.size() * 72L + characters * 2L;
		return new Diagnostics(queries, candidates, misses, staleRemovals,
				byIdentity.size(), byName.size(), estimatedBytes);
	}

	public void clear() {
		byName.clear();
		byIdentity.clear();
		queries = 0L;
		candidates = 0L;
		misses = 0L;
		staleRemovals = 0L;
	}

	private static String normalize(String name) {
		return AuthenticatedName.canonical(name);
	}
}

package com.powers.companion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Bounded registry-name resolver; ambiguity is returned rather than guessed. */
public final class ShadowNameResolver {
	public enum Type { POWER, ITEM }
	public enum Status { FOUND, MISSING, AMBIGUOUS }
	public record Resolution(Status status, String value, List<String> candidates) {
		public Resolution {
			value = value == null ? "" : value;
			candidates = List.copyOf(candidates);
		}
	}

	private final Map<String, String> powers;
	private final Map<String, String> items;

	private ShadowNameResolver(Map<String, String> powers, Map<String, String> items) {
		this.powers = normalized(powers);
		this.items = normalized(items);
	}

	public static ShadowNameResolver from(Map<String, String> powers, Map<String, String> items) {
		return new ShadowNameResolver(powers, items);
	}

	public Resolution resolve(Type type, String raw) {
		String query = normalize(raw);
		if (query.isEmpty()) return new Resolution(Status.MISSING, "", List.of());
		Map<String, String> names = type == Type.POWER ? powers : items;
		String exact = names.get(query);
		if (exact != null) return new Resolution(Status.FOUND, exact, List.of(exact));
		LinkedHashSet<String> candidates = new LinkedHashSet<>();
		for (Map.Entry<String, String> entry : names.entrySet()) {
			if (entry.getKey().contains(query) || query.contains(entry.getKey())) {
				candidates.add(entry.getValue());
				if (candidates.size() == 6) break;
			}
		}
		if (candidates.size() == 1) {
			String value = candidates.iterator().next();
			return new Resolution(Status.FOUND, value, List.of(value));
		}
		List<String> values = new ArrayList<>(candidates);
		return new Resolution(values.isEmpty() ? Status.MISSING : Status.AMBIGUOUS,
				"", values);
	}

	private static Map<String, String> normalized(Map<String, String> source) {
		Map<String, String> result = new LinkedHashMap<>();
		if (source != null) source.forEach((key, value) -> {
			String normalized = normalize(key);
			if (!normalized.isEmpty() && value != null && !value.isBlank()) {
				result.put(normalized, value.strip());
			}
		});
		return Map.copyOf(result);
	}

	private static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT)
				.replace('_', ' ').replaceAll("[^a-z0-9: ]", " ")
				.replaceAll("\\s+", " ").strip();
	}
}

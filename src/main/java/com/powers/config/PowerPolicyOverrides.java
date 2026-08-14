package com.powers.config;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Bounded deterministic world and dimension policy patches. */
public record PowerPolicyOverrides(
		Map<String, PowerPolicyPatch> worlds,
		Map<String, PowerPolicyPatch> dimensions) {
	public static final int MAX_PER_SCOPE = 128;

	public PowerPolicyOverrides {
		worlds = sanitizedMap(worlds, false);
		dimensions = sanitizedMap(dimensions, true);
	}

	public static PowerPolicyOverrides empty() {
		return new PowerPolicyOverrides(Map.of(), Map.of());
	}

	static boolean validWorldKey(String key) {
		return key != null && !key.isBlank() && key.length() <= 128
				&& key.codePoints().noneMatch(Character::isISOControl);
	}

	static boolean validDimensionKey(String key) {
		if (key == null) return false;
		String normalized = key.strip();
		return normalized.length() <= 128
				&& normalized.matches("[a-z0-9_.-]+:[a-z0-9_./-]+");
	}

	private static Map<String, PowerPolicyPatch> sanitizedMap(
			Map<String, PowerPolicyPatch> input, boolean dimension) {
		if (input == null || input.isEmpty()) return Map.of();
		TreeMap<String, PowerPolicyPatch> accepted = new TreeMap<>();
		for (Map.Entry<String, PowerPolicyPatch> entry : input.entrySet()) {
			if (accepted.size() >= MAX_PER_SCOPE || entry.getKey() == null
					|| entry.getValue() == null || entry.getValue().isEmpty()) continue;
			String key = dimension ? entry.getKey().strip() : entry.getKey();
			boolean valid = dimension ? validDimensionKey(key) : validWorldKey(key);
			if (valid) accepted.put(key, entry.getValue());
		}
		return accepted.isEmpty() ? Map.of()
				: Collections.unmodifiableMap(accepted);
	}
}

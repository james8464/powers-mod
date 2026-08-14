package com.powers.magic;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** One immutable, fully validated view of every canonical action and retired-key alias. */
public record ActionRegistrySnapshot(long revision, Map<MagicActionId, MagicActionDefinition> definitions,
		Map<String, String> aliases, ValidationState validation) {
	public static final int MAX_ALIASES = 256;
	public static final int MAX_ALIAS_DEPTH = 16;

	public record ValidationState(boolean valid, int canonicalKeyCount, int aliasCount) { }

	public ActionRegistrySnapshot {
		if (revision < 0L) throw new IllegalArgumentException("Registry revision cannot be negative");
		definitions = Collections.unmodifiableMap(
				new LinkedHashMap<>(Objects.requireNonNull(definitions, "definitions")));
		aliases = Collections.unmodifiableMap(
				new LinkedHashMap<>(Objects.requireNonNull(aliases, "aliases")));
		validation = Objects.requireNonNull(validation, "validation");
		if (!validation.valid() || validation.canonicalKeyCount() != definitions.size()
				|| validation.aliasCount() != aliases.size() || aliases.size() > MAX_ALIASES) {
			throw new IllegalArgumentException("Action registry validation state does not match its content");
		}
		for (String alias : aliases.keySet()) {
			if (definitions.containsKey(new MagicActionId(alias))) {
				throw new IllegalArgumentException("Alias collides with canonical action: " + alias);
			}
			resolve(definitions, aliases, alias);
		}
	}

	static ActionRegistrySnapshot validated(long revision,
			Collection<MagicActionDefinition> definitions, Map<String, String> aliases) {
		Objects.requireNonNull(definitions, "definitions");
		Objects.requireNonNull(aliases, "aliases");
		if (aliases.size() > MAX_ALIASES) throw new IllegalArgumentException("Too many action aliases");
		Map<MagicActionId, MagicActionDefinition> indexed = new LinkedHashMap<>();
		for (MagicActionDefinition definition : definitions) {
			if (indexed.putIfAbsent(definition.id(), definition) != null) {
				throw new IllegalArgumentException("Duplicate magic action: " + definition.id());
			}
		}
		Map<String, String> copied = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : aliases.entrySet()) {
			String alias = validKey(entry.getKey(), "alias");
			String target = validKey(entry.getValue(), "target");
			if (indexed.containsKey(new MagicActionId(alias))) {
				throw new IllegalArgumentException("Alias collides with canonical action: " + alias);
			}
			if (copied.putIfAbsent(alias, target) != null) {
				throw new IllegalArgumentException("Duplicate action alias: " + alias);
			}
		}
		for (String alias : copied.keySet()) resolve(indexed, copied, alias);
		return new ActionRegistrySnapshot(revision, indexed, copied,
				new ValidationState(true, indexed.size(), copied.size()));
	}

	private static String validKey(String value, String kind) {
		if (value == null || value.isBlank() || value.length() > 96
				|| !value.matches("[a-z0-9_./-]+")) {
			throw new IllegalArgumentException("Invalid action " + kind);
		}
		return value;
	}

	/** Resolves a canonical or retired key, returning {@code null} for unknown input. */
	public MagicActionId resolve(String key) {
		if (key == null || key.isBlank() || key.length() > 96) return null;
		try {
			return resolve(definitions, aliases, key);
		} catch (IllegalArgumentException invalid) {
			return null;
		}
	}

	private static MagicActionId resolve(Map<MagicActionId, MagicActionDefinition> definitions,
			Map<String, String> aliases, String key) {
		String current = key;
		Set<String> visited = new LinkedHashSet<>();
		for (int depth = 0; depth <= MAX_ALIAS_DEPTH; depth++) {
			MagicActionId id = new MagicActionId(current);
			if (definitions.containsKey(id)) return id;
			if (!visited.add(current)) throw new IllegalArgumentException("Cyclic action alias: " + key);
			String next = aliases.get(current);
			if (next == null) throw new IllegalArgumentException("Unknown action alias target: " + current);
			current = next;
		}
		throw new IllegalArgumentException("Action alias chain exceeds " + MAX_ALIAS_DEPTH);
	}

	public MagicActionDefinition definition(MagicActionId id) {
		return definitions.get(Objects.requireNonNull(id, "id"));
	}

	public List<MagicActionDefinition> orderedDefinitions() {
		return List.copyOf(definitions.values());
	}
}

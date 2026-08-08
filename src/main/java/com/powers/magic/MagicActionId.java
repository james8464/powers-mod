package com.powers.magic;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable namespace-local identity for a magical action. These values are
 * persisted in generated interaction documentation, so renaming one requires
 * an explicit migration rather than an incidental refactor.
 *
 * @param value lowercase path using Minecraft identifier-safe characters
 */
public record MagicActionId(String value) implements Comparable<MagicActionId> {
	private static final Pattern VALID = Pattern.compile("[a-z0-9_.-]+");

	/** Validates and creates a stable action identity. */
	public MagicActionId {
		Objects.requireNonNull(value, "value");
		if (!VALID.matcher(value).matches()) {
			throw new IllegalArgumentException("Invalid magic action id: " + value);
		}
	}

	@Override
	public int compareTo(MagicActionId other) {
		return value.compareTo(other.value);
	}

	@Override
	public String toString() {
		return value;
	}
}

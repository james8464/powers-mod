package com.powers.spell;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Strict, bounded grammar accepted by Cartographer's Star. */
public record CartographerQuery(Kind kind, String target) {
	public static final int MAX_LENGTH = 64;
	private static final Pattern IDENTIFIER = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
	private static final Pattern LANDMARK = Pattern.compile("[a-z0-9_ -]+");

	public enum Kind { STRUCTURE, BIOME, LANDMARK }

	public CartographerQuery {
		if (kind == null || target == null || target.isBlank()) {
			throw new IllegalArgumentException("Cartographer queries require a kind and target");
		}
	}

	public static Optional<CartographerQuery> parse(String raw) {
		if (raw == null) return Optional.empty();
		String input = raw.trim().toLowerCase(Locale.ROOT);
		if (input.isEmpty() || input.length() > MAX_LENGTH) return Optional.empty();
		int separator = input.indexOf(' ');
		if (separator <= 0 || separator == input.length() - 1) return Optional.empty();
		Kind kind;
		try {
			kind = Kind.valueOf(input.substring(0, separator).toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ignored) {
			return Optional.empty();
		}
		String target = input.substring(separator + 1).trim();
		if (kind == Kind.LANDMARK) {
			if (!LANDMARK.matcher(target).matches()) return Optional.empty();
			target = target.replace('-', '_').replace(' ', '_');
			target = target.replaceAll("_+", "_");
		} else if (!IDENTIFIER.matcher(target).matches()) {
			return Optional.empty();
		}
		return Optional.of(new CartographerQuery(kind, target));
	}
}

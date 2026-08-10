package com.powers.entity;

import java.util.Locale;
import java.util.UUID;

/** Valid Minecraft-style usernames for saved Power Test Actors. */
public final class TestActorIdentity {
	private static final int MAX_LENGTH = 16;

	private TestActorIdentity() {
	}

	/** Derives a stable name without claiming a real Minecraft account. */
	public static String defaultUsername(UUID id) {
		String compact = id.toString().replace("-", "").toLowerCase(Locale.ROOT);
		return "Test_" + compact.substring(0, 8);
	}

	/** Converts user or save input into a bounded username, falling back safely. */
	public static String normalize(String requested, UUID id) {
		if (requested == null) return defaultUsername(id);
		String trimmed = requested.trim();
		StringBuilder normalized = new StringBuilder(MAX_LENGTH);
		for (int i = 0; i < trimmed.length() && normalized.length() < MAX_LENGTH; i++) {
			char character = trimmed.charAt(i);
			if (Character.isLetterOrDigit(character) || character == '_') normalized.append(character);
			else if (Character.isWhitespace(character) || character == '-') normalized.append('_');
		}
		return normalized.isEmpty() ? defaultUsername(id) : normalized.toString();
	}
}

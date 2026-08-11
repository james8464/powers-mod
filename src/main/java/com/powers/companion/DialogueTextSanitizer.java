package com.powers.companion;

import java.util.regex.Pattern;

/**
 * Converts untrusted dialogue into a single, literal chat line.
 *
 * <p>Remote prose never carries formatting or command authority, but removing
 * controls here also prevents it from impersonating chat speakers or hiding
 * text with bidirectional overrides.</p>
 */
public final class DialogueTextSanitizer {
	private static final Pattern LEGACY_FORMATTING = Pattern.compile("(?i)\u00a7[0-9A-FK-ORX]");
	private static final Pattern DIRECTIONAL_CONTROLS = Pattern.compile(
			"[\u061c\u200e\u200f\u202a-\u202e\u2066-\u2069]");
	private static final Pattern OTHER_CONTROLS = Pattern.compile("[\\p{Cc}\\p{Cf}]");
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private DialogueTextSanitizer() {
	}

	/** Returns bounded plain text, or a separately sanitized fallback. */
	public static String sanitize(String value, int maximumCharacters, String fallback) {
		int limit = Math.max(1, maximumCharacters);
		String safeFallback = plain(fallback);
		if (safeFallback.isEmpty()) safeFallback = "...";
		String safe = plain(value);
		if (safe.isEmpty()) safe = safeFallback;
		return safe.substring(0, Math.min(limit, safe.length())).strip();
	}

	private static String plain(String value) {
		if (value == null) return "";
		String safe = LEGACY_FORMATTING.matcher(value).replaceAll("");
		safe = DIRECTIONAL_CONTROLS.matcher(safe).replaceAll("");
		safe = OTHER_CONTROLS.matcher(safe).replaceAll(" ");
		// Angle-bracket speaker prefixes are presentation, never remote authority.
		safe = safe.replace("<", "").replace(">", "");
		return WHITESPACE.matcher(safe).replaceAll(" ").strip();
	}
}

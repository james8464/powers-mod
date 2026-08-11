package com.powers.knowledge;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Immutable, privacy-safe facts about one server-authoritative magical action. */
public record MagicAttempt(String actionId, MagicFailureReason reason, long gameTick,
		Map<String, Long> facts) {
	private static final Set<String> ALLOWED_FACTS = Set.of(
			"required", "available", "remaining_ticks", "current_rank", "required_rank",
			"distance", "range", "budget", "repeat_count");

	public MagicAttempt {
		actionId = canonicalAction(actionId);
		reason = reason == null ? MagicFailureReason.EXECUTION_FAILED : reason;
		gameTick = Math.max(0L, gameTick);
		Map<String, Long> safe = new LinkedHashMap<>();
		if (facts != null) {
			facts.forEach((key, value) -> {
				if (key != null && value != null && ALLOWED_FACTS.contains(key)
						&& safe.size() < 8) safe.put(key, value);
			});
		}
		facts = Map.copyOf(safe);
	}

	public static MagicAttempt failure(String actionId, MagicFailureReason reason,
			long gameTick, Map<String, Long> facts) {
		if (reason == MagicFailureReason.NONE) {
			throw new IllegalArgumentException("A failure needs a failure reason");
		}
		return new MagicAttempt(actionId, reason, gameTick, facts);
	}

	public static MagicAttempt success(String actionId, long gameTick) {
		return new MagicAttempt(actionId, MagicFailureReason.NONE, gameTick, Map.of());
	}

	public boolean succeeded() {
		return reason == MagicFailureReason.NONE;
	}

	static String canonicalAction(String value) {
		String safe = value == null ? "magic" : value.toLowerCase(java.util.Locale.ROOT)
				.replaceAll("[^a-z0-9_:/.-]", "_").replaceAll("_+", "_");
		if (safe.isBlank()) safe = "magic";
		return safe.substring(0, Math.min(96, safe.length()));
	}
}

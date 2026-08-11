package com.powers.knowledge;

import java.util.List;
import java.util.Locale;

/** Deterministic wording for Shadow's authoritative recent-action diagnosis. */
public final class MagicDiagnosticAnswer {
	private MagicDiagnosticAnswer() {
	}

	public static KnowledgeAnswer answer(MagicAttempt attempt) {
		return new KnowledgeAnswer("recent_magic_diagnostic", text(attempt), 1.0,
				List.of("server-authoritative magic attempt journal"), List.of());
	}

	public static String text(MagicAttempt attempt) {
		String action = display(attempt.actionId());
		long required = fact(attempt, "required");
		long available = fact(attempt, "available");
		long remaining = fact(attempt, "remaining_ticks");
		return switch (attempt.reason()) {
			case NONE -> "Your " + action + " succeeded.";
			case NO_TARGET -> failed(action, "it could not find a valid target");
			case INSUFFICIENT_ENERGY -> "Your " + action + " failed because it required "
					+ required + " energy, but only " + available + " was available.";
			case COOLDOWN -> failed(action, "its cooldown has " + ((remaining + 19L) / 20L)
					+ " seconds remaining");
			case AMETHYST -> failed(action, "amethyst suppressed the magic");
			case SAFE_ZONE -> failed(action, "the destination or target was protected by a safe zone");
			case CONSENT -> failed(action, "the target did not grant consent");
			case RANK_LOCK -> failed(action, "it requires rank " + fact(attempt, "required_rank")
					+ ", while your effective rank is " + fact(attempt, "current_rank"));
			case ALIGNMENT_LOCK -> failed(action, "your current alignment cannot wield it");
			case WRONG_DIMENSION -> failed(action, "it cannot operate in this dimension");
			case OUT_OF_RANGE -> failed(action, "the target was beyond its " + fact(attempt, "range")
					+ " block range");
			case BLOCKED_LINE_OF_SIGHT -> failed(action, "solid terrain blocked its line of sight");
			case CHANNEL_INTERRUPTED -> failed(action, "its ritual channel was interrupted");
			case SERVER_BUDGET -> failed(action, "the server's bounded magic workload was already full");
			case TIME_LOCKED -> failed(action, "time was frozen for you");
			case MAGIC_COLLISION -> failed(action, "another magical presence cancelled it");
			case ALREADY_CHANNELING -> failed(action, "you were already channeling another ritual");
			case INVALID_INPUT -> failed(action, "its selected input was invalid");
			case EXECUTION_FAILED -> failed(action, "its world conditions changed before it completed");
		};
	}

	private static String failed(String action, String cause) {
		return "Your " + action + " failed because " + cause + ".";
	}

	private static long fact(MagicAttempt attempt, String key) {
		return Math.max(0L, attempt.facts().getOrDefault(key, 0L));
	}

	private static String display(String actionId) {
		String value = actionId;
		int separator = Math.max(value.lastIndexOf(':'), value.lastIndexOf('/'));
		if (separator >= 0 && separator + 1 < value.length()) value = value.substring(separator + 1);
		String[] words = value.replace('.', '_').split("_");
		StringBuilder result = new StringBuilder();
		for (String word : words) {
			if (word.isBlank()) continue;
			if (!result.isEmpty()) result.append(' ');
			result.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
		}
		return result.isEmpty() ? "Magic" : result.toString();
	}
}

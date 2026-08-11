package com.powers.diagnostics;

import com.powers.knowledge.MagicAttempt;

import java.util.Comparator;
import java.util.Map;

/** Bounded redacted values suitable for Minecraft's system crash report. */
public record CrashDiagnosticSection(String activeSessions, String lastTypedFailure) {
	private static final int SESSION_LIMIT = 256;
	private static final int FAILURE_LIMIT = 160;

	public static CrashDiagnosticSection create(Map<String, Integer> sessions, MagicAttempt failure) {
		String active = sessions == null ? "none" : sessions.entrySet().stream()
				.sorted(Map.Entry.comparingByKey(Comparator.naturalOrder())).limit(12)
				.map(entry -> safeKey(entry.getKey()) + "=" + Math.max(0, entry.getValue()))
				.reduce((left, right) -> left + "; " + right).orElse("none");
		String typed = failure == null || failure.succeeded() ? "none"
				: "reason=" + failure.reason().name().toLowerCase(java.util.Locale.ROOT)
				+ "; tick=" + failure.gameTick();
		return new CrashDiagnosticSection(bound(active, SESSION_LIMIT), bound(typed, FAILURE_LIMIT));
	}

	private static String safeKey(String value) {
		if (value == null) return "session";
		String safe = value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
		return bound(safe.isBlank() ? "session" : safe, 32);
	}

	private static String bound(String value, int maximum) {
		String oneLine = value.replace('\n', '_').replace('\r', '_').replace('\t', '_');
		return oneLine.substring(0, Math.min(maximum, oneLine.length()));
	}
}

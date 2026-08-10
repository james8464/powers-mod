package com.powers.knowledge;

import java.util.Locale;

/** Pure policy preventing an optional remote provider from inventing recipes. */
public final class KnowledgeRemoteRules {
	private KnowledgeRemoteRules() {
	}

	public static boolean mayFallback(String question, double offlineConfidence) {
		if (question == null || question.isBlank() || !Double.isFinite(offlineConfidence)
				|| offlineConfidence >= 0.5) return false;
		String normalized = question.toLowerCase(Locale.ROOT);
		return !normalized.contains("recipe") && !normalized.contains("craft")
				&& !normalized.contains("make ");
	}
}

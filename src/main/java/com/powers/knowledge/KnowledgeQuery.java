package com.powers.knowledge;

import java.util.List;

/** Redacted question context safe for offline providers and optional remote fallback. */
public record KnowledgeQuery(String question, int revealRank, List<String> contextRegistryIds,
		String authoritativeDiagnostic) {
	public KnowledgeQuery(String question, int revealRank, List<String> contextRegistryIds) {
		this(question, revealRank, contextRegistryIds, "");
	}

	public KnowledgeQuery {
		question = question == null ? "" : question.strip();
		if (question.length() > 256) question = question.substring(0, 256);
		revealRank = Math.clamp(revealRank, 0, 10);
		contextRegistryIds = contextRegistryIds == null ? List.of() : List.copyOf(contextRegistryIds);
		authoritativeDiagnostic = authoritativeDiagnostic == null ? "" : authoritativeDiagnostic.strip();
		if (authoritativeDiagnostic.length() > 512) {
			authoritativeDiagnostic = authoritativeDiagnostic.substring(0, 512);
		}
	}
}

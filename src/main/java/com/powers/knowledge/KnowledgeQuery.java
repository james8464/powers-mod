package com.powers.knowledge;

import java.util.List;

/** Redacted question context safe for offline providers and optional remote fallback. */
public record KnowledgeQuery(String question, int revealRank, List<String> contextRegistryIds) {
	public KnowledgeQuery {
		question = question == null ? "" : question.strip();
		if (question.length() > 256) question = question.substring(0, 256);
		revealRank = Math.clamp(revealRank, 0, 10);
		contextRegistryIds = contextRegistryIds == null ? List.of() : List.copyOf(contextRegistryIds);
	}
}

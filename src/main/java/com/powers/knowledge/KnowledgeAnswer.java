package com.powers.knowledge;

import java.util.List;

/** One bounded, sourced answer spoken by Shadow. */
public record KnowledgeAnswer(String entryId, String answer, double confidence,
		List<String> sources, List<String> registryIds) {
	public KnowledgeAnswer {
		entryId = entryId == null ? "" : entryId;
		answer = answer == null ? "" : answer;
		confidence = Math.clamp(Double.isFinite(confidence) ? confidence : 0.0, 0.0, 1.0);
		sources = sources == null ? List.of() : List.copyOf(sources);
		registryIds = registryIds == null ? List.of() : List.copyOf(registryIds);
	}
}

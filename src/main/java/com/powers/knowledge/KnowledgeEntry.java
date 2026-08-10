package com.powers.knowledge;

import java.util.List;

/** Datapack-authored, source-cited offline answer with an optional spoiler floor. */
public record KnowledgeEntry(String id, String title, List<String> keywords,
		String answer, List<String> sources, int revealRank) {
	public KnowledgeEntry {
		if (id == null || id.isBlank() || id.length() > 96) {
			throw new IllegalArgumentException("Knowledge entry id must contain 1..96 characters");
		}
		if (title == null || title.isBlank() || answer == null || answer.isBlank()) {
			throw new IllegalArgumentException("Knowledge title and answer are required");
		}
		keywords = keywords == null ? List.of() : List.copyOf(keywords);
		sources = sources == null ? List.of() : List.copyOf(sources);
		revealRank = Math.clamp(revealRank, 0, 10);
	}
}

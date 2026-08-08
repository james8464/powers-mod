package com.powers.progression;

import java.util.List;

/** One validated rank-maze choice and its prerequisite node identifiers. */
public record RankNode(String id, int depth, String branch, String title,
		List<String> parents, boolean canonical) {
	public RankNode {
		if (id == null || id.isBlank() || branch == null || title == null || depth < 0) {
			throw new IllegalArgumentException("Invalid rank node");
		}
		parents = parents == null ? List.of() : List.copyOf(parents);
	}
}

package com.powers.progression;

import java.util.List;

/** One validated rank-maze choice and its prerequisite node identifiers. */
public record RankNode(String id, int depth, String branch, String title,
		List<String> parents, boolean canonical, List<RankPerk> perks) {
	public RankNode {
		if (id == null || id.isBlank() || branch == null || title == null || depth < 0) {
			throw new IllegalArgumentException("Invalid rank node");
		}
		parents = parents == null ? List.of() : List.copyOf(parents);
		perks = perks == null ? List.of() : List.copyOf(perks);
	}

	/** Compatibility constructor for small synthetic graphs that do not model perks. */
	public RankNode(String id, int depth, String branch, String title, List<String> parents, boolean canonical) {
		this(id, depth, branch, title, parents, canonical, List.of());
	}
}

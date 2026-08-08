package com.powers.progression;

import java.util.LinkedHashSet;
import java.util.Set;

public record RankProgress(Set<String> completed, String focus) {
	public RankProgress {
		completed = Set.copyOf(completed);
	}

	public int depth(RankGraph graph) {
		return completed.stream().map(graph::node).filter(java.util.Objects::nonNull)
				.mapToInt(RankNode::depth).max().orElse(0);
	}

	public static RankProgress migrateLegacy(RankGraph graph, int legacyLevel) {
		Set<String> migrated = new LinkedHashSet<>();
		String focus = "";
		for (RankNode node : graph.nodes().stream()
				.filter(RankNode::canonical)
				.sorted(java.util.Comparator.comparingInt(RankNode::depth)).toList()) {
			if (node.depth() <= legacyLevel && migrated.containsAll(node.parents())) {
				migrated.add(node.id());
				focus = node.id();
			}
		}
		return new RankProgress(migrated, focus);
	}
}

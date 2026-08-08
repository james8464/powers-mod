package com.powers.progression;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Immutable validated directed acyclic graph for one rank maze, including
 * prerequisite and earned-depth unlock calculations.
 */
public final class RankGraph {
	private final Map<String, RankNode> nodes;

	public RankGraph(Collection<RankNode> nodes) {
		Map<String, RankNode> indexed = new HashMap<>();
		for (RankNode node : nodes) {
			if (indexed.putIfAbsent(node.id(), node) != null) {
				throw new IllegalArgumentException("Duplicate rank node: " + node.id());
			}
		}
		if (indexed.isEmpty()) throw new IllegalArgumentException("Rank graph is empty");
		for (RankNode node : indexed.values()) {
			for (String parent : node.parents()) {
				RankNode parentNode = indexed.get(parent);
				if (parentNode == null) throw new IllegalArgumentException("Missing parent: " + parent);
				if (parentNode.depth() >= node.depth()) {
					throw new IllegalArgumentException("Parents must be in an earlier depth band");
				}
			}
		}
		for (String id : indexed.keySet()) visit(id, indexed, new HashSet<>(), new HashSet<>());
		this.nodes = Map.copyOf(indexed);
	}

	public RankNode node(String id) {
		return nodes.get(id);
	}

	public Collection<RankNode> nodes() {
		return nodes.values();
	}

	public Set<String> unlockable(Set<String> completed, int earnedDepth) {
		Set<String> result = new LinkedHashSet<>();
		for (RankNode node : nodes.values().stream()
				.sorted(java.util.Comparator.comparingInt(RankNode::depth).thenComparing(RankNode::id)).toList()) {
			if (node.depth() <= earnedDepth && !completed.contains(node.id())
					&& completed.containsAll(node.parents())) result.add(node.id());
		}
		return Set.copyOf(result);
	}

	private static void visit(String id, Map<String, RankNode> nodes, Set<String> visiting, Set<String> done) {
		if (done.contains(id)) return;
		if (!visiting.add(id)) throw new IllegalArgumentException("Cycle at " + id);
		for (String parent : nodes.get(id).parents()) visit(parent, nodes, visiting, done);
		visiting.remove(id);
		done.add(id);
	}
}

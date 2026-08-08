package com.powers.progression;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankGraphTest {
	@Test
	void supportsBranchesAndReconvergence() {
		RankGraph graph = new RankGraph(List.of(
				node("root", 0, List.of()),
				node("might", 1, List.of("root")),
				node("insight", 1, List.of("root")),
				node("weaver", 2, List.of("might", "insight"))));

		assertEquals(Set.of("might", "insight"), graph.unlockable(Set.of("root"), 2));
		assertTrue(graph.unlockable(Set.of("root", "might", "insight"), 2).contains("weaver"));
	}

	@Test
	void rejectsMissingParentsAndCycles() {
		assertThrows(IllegalArgumentException.class, () -> new RankGraph(List.of(
				node("orphan", 1, List.of("missing")))));
		assertThrows(IllegalArgumentException.class, () -> new RankGraph(List.of(
				node("a", 1, List.of("b")), node("b", 1, List.of("a")))));
	}

	private static RankNode node(String id, int depth, List<String> parents) {
		return new RankNode(id, depth, "insight", id, parents, false);
	}
}

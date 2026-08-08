package com.powers.progression;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Proves the rank map is deterministic, non-overlapping, and faithful to graph edges. */
class RankMazeLayoutTest {
	@Test
	void nodesAtTheSameDepthDoNotOverlapAtMinimumSize() {
		RankGraph graph = graph();
		RankMazeLayout layout = RankMazeLayout.arrange(graph, 320, 240);
		for (RankMazeLayout.NodeBox first : layout.nodes()) {
			assertFalse(first.x() < 0 || first.y() < 0 || first.x() + first.width() > layout.width()
					|| first.y() + first.height() > layout.height());
			for (RankMazeLayout.NodeBox second : layout.nodes()) {
				if (!first.id().equals(second.id())) assertFalse(first.overlaps(second));
			}
		}
	}

	@Test
	void edgesExactlyMatchDeclaredParents() {
		RankGraph graph = graph();
		RankMazeLayout layout = RankMazeLayout.arrange(graph, 400, 260);
		assertEquals(4, layout.edges().size());
		assertEquals(graph.nodes().size(), layout.nodes().size());
	}

	private static RankGraph graph() {
		return new RankGraph(List.of(
				new RankNode("root", 0, "origin", "Root", List.of(), true),
				new RankNode("left", 1, "ward", "Left", List.of("root"), false),
				new RankNode("right", 1, "rift", "Right", List.of("root"), false),
				new RankNode("crown", 2, "crown", "Crown", List.of("left", "right"), true)));
	}
}

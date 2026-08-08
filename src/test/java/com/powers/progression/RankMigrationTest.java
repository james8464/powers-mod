package com.powers.progression;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankMigrationTest {
	@Test
	void everyLegacyLevelMigratesWithoutLosingDepth() {
		ArrayList<RankNode> nodes = new ArrayList<>();
		for (int depth = 0; depth <= 10; depth++) {
			nodes.add(new RankNode("legacy_" + depth, depth, "legacy", "Tier " + depth,
					depth == 0 ? java.util.List.of() : java.util.List.of("legacy_" + (depth - 1)), true));
		}
		RankGraph graph = new RankGraph(nodes);
		for (int level = 0; level <= 10; level++) {
			RankProgress progress = RankProgress.migrateLegacy(graph, level);
			assertEquals(level, progress.depth(graph));
			assertTrue(progress.completed().contains("legacy_" + level));
		}
	}
}

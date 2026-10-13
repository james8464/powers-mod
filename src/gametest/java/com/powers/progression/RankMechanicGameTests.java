package com.powers.progression;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.HashSet;
import java.util.List;

/** Live-server proof that both loaded mazes expose executable numerical mechanics. */
public final class RankMechanicGameTests {
	@GameTest(maxTicks = 20)
	public void allFiftySixLoadedNodesExecuteNamedFiniteProfiles(GameTestHelper helper) {
		var identities = new HashSet<String>();
		var graphs = List.of(RankGraphRegistry.light(), RankGraphRegistry.darkness());
		for (int alignment = 0; alignment < graphs.size(); alignment++) {
			RankGraph graph = graphs.get(alignment);
			helper.assertTrue(graph != null, "Rank graph was not initialized on the live server");
			for (RankNode node : graph.nodes()) {
				helper.assertTrue(identities.add(alignment + ":" + node.id()),
						"Duplicate loaded rank-node identity: " + node.id());
				helper.assertTrue(!node.perks().isEmpty(),
						"Loaded rank node has no mechanic: " + node.id());
				for (RankPerk perk : node.perks()) {
					double active = perk.execute(100.0, false);
					double focused = perk.execute(100.0, true);
					helper.assertTrue(!perk.mechanicName().isBlank(),
							"Loaded rank mechanic is unnamed: " + node.id());
					helper.assertTrue(Double.isFinite(active) && active != 100.0,
							"Loaded rank mechanic is not executable: " + node.id());
					helper.assertTrue(Double.isFinite(focused)
								&& Math.abs(focused - 100.0) >= Math.abs(active - 100.0),
							"Loaded focused mechanic is invalid: " + node.id());
				}
			}
		}
		helper.assertTrue(identities.size() == 56,
				"Expected 56 loaded rank nodes, found " + identities.size());
		helper.succeed();
	}
}

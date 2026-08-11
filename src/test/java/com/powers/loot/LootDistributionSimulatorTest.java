package com.powers.loot;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LootDistributionSimulatorTest {
	@Test
	void everyInjectedPoolMatchesItsAuthoredProbabilityAndIgnoresForeignWeights() {
		for (LootDropGroup group : LootInjectionCatalog.groups()) {
			var baseline = LootDistributionSimulator.simulate(group, 200_000, 0, 0x504F57455253L);
			var heavyPack = LootDistributionSimulator.simulate(group, 200_000, 50_000, 0x504F57455253L);
			double tolerance = Math.max(0.0025, 5.0 * baseline.standardError());
			assertEquals(group.chance(), baseline.dropRate(), tolerance, group.tableId());
			assertEquals(baseline.dropRate(), heavyPack.dropRate(), 0.0, group.tableId());
			assertEquals(baseline.itemCounts(), heavyPack.itemCounts(), group.tableId());
			assertTrue(baseline.itemCounts().keySet().containsAll(group.itemIds()), group.tableId());
		}
	}

	@Test
	void countRangeAndEqualItemWeightsAreReproducible() {
		var group = new LootDropGroup("test:table", 1.0F, 2, 4, List.of("a", "b", "c"));
		var first = LootDistributionSimulator.simulate(group, 30_000, 500, 42L);
		var again = LootDistributionSimulator.simulate(group, 30_000, 0, 42L);
		assertEquals(first, again);
		assertTrue(first.totalItems() >= 60_000 && first.totalItems() <= 120_000);
		for (long count : first.itemCounts().values()) {
			assertEquals(first.totalItems() / 3.0, count, first.totalItems() * 0.015);
		}
	}
}

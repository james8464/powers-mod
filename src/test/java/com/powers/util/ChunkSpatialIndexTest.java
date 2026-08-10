package com.powers.util;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkSpatialIndexTest {
	@Test
	void nearbyUsesDimensionAndExactRadiusWhileReplacementRemovesGhostCells() {
		ChunkSpatialIndex<String, String> index = new ChunkSpatialIndex<>(16);
		index.put("near", "overworld", 8.0, 8.0, 3.0, "near-value");
		index.put("far", "overworld", 96.0, 96.0, 2.0, "far-value");
		index.put("other", "nether", 8.0, 8.0, 3.0, "other-value");

		assertEquals(Set.of("near-value"), Set.copyOf(index.nearby("overworld", 0.0, 0.0, 16.0)));

		index.put("near", "overworld", 80.0, 80.0, 2.0, "moved-value");
		assertTrue(index.nearby("overworld", 0.0, 0.0, 16.0).isEmpty());
		assertEquals(Set.of("moved-value"), Set.copyOf(index.nearby("overworld", 80.0, 80.0, 4.0)));
	}

	@Test
	void removalAndClearReleaseEveryMembership() {
		ChunkSpatialIndex<String, Integer> index = new ChunkSpatialIndex<>(16);
		index.put("wide", "overworld", 16.0, 16.0, 24.0, 1);
		index.put("small", "overworld", 64.0, 64.0, 1.0, 2);

		assertEquals(2, index.size());
		assertTrue(index.remove("wide"));
		assertEquals(1, index.size());
		index.clear();
		assertEquals(0, index.size());
		assertEquals(0, index.cellCount());
	}
}

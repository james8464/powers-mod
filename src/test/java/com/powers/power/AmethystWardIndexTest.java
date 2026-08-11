package com.powers.power;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmethystWardIndexTest {
	@Test
	void nearbyQueryTouchesOnlyIntersectingChunkBuckets() {
		AmethystWardIndex index = new AmethystWardIndex();
		BlockPos origin = new BlockPos(1, 64, 1);
		BlockPos adjacentChunk = new BlockPos(17, 64, 1);
		BlockPos remote = new BlockPos(400, 64, 400);
		index.add(origin);
		index.add(adjacentChunk);
		index.add(remote);

		assertEquals(Set.of(origin, adjacentChunk), Set.copyOf(index.nearby(origin, 20)));
		assertEquals(3, index.size());
	}

	@Test
	void removalDropsEmptyBucketsWithoutAFullIndexScan() {
		AmethystWardIndex index = new AmethystWardIndex();
		BlockPos ward = new BlockPos(-17, 20, -17);
		index.add(ward);
		index.remove(ward);
		assertTrue(index.nearby(ward, 64).isEmpty());
		assertEquals(0, index.size());
	}

	@Test
	void diagnosticsExposeCandidatesMissesAndStaleRemovals() {
		AmethystWardIndex index = new AmethystWardIndex();
		BlockPos ward = new BlockPos(1, 64, 1);
		index.add(ward);
		assertEquals(1, index.nearby(ward, 8).size());
		assertTrue(index.nearby(new BlockPos(500, 64, 500), 8).isEmpty());
		index.removeStale(ward);

		var diagnostics = index.diagnostics();
		assertEquals(2, diagnostics.queries());
		assertEquals(1, diagnostics.candidates());
		assertEquals(1, diagnostics.misses());
		assertEquals(1, diagnostics.staleRemovals());
		assertTrue(diagnostics.estimatedBytes() >= 0);
	}
}

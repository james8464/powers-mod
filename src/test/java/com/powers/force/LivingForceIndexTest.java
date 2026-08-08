package com.powers.force;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.powers.force.LivingForceKind.DARKNESS;
import static com.powers.force.LivingForceKind.PURE_LIGHT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LivingForceIndexTest {
	@Test
	void rangeQueriesReturnOnlyMatchingInSpherePositions() {
		LivingForceIndex index = new LivingForceIndex();
		long closeDarkness = BlockPos.asLong(3, 64, 4);
		long farDarkness = BlockPos.asLong(20, 64, 0);
		long closeLight = BlockPos.asLong(1, 64, 1);
		index.add(closeDarkness, DARKNESS);
		index.add(farDarkness, DARKNESS);
		index.add(closeLight, PURE_LIGHT);

		assertEquals(List.of(closeDarkness), index.within(0.5, 64.5, 0.5, 5.0, DARKNESS));
		assertEquals(List.of(closeLight), index.within(0.5, 64.5, 0.5, 5.0, PURE_LIGHT));
	}

	@Test
	void movingAForceUpdatesItsKindWithoutDuplicatingPosition() {
		LivingForceIndex index = new LivingForceIndex();
		long position = BlockPos.asLong(-1, 20, -1);
		index.add(position, DARKNESS);
		index.add(position, PURE_LIGHT);

		assertTrue(index.within(-0.5, 20.5, -0.5, 1.0, DARKNESS).isEmpty());
		assertEquals(List.of(position), index.within(-0.5, 20.5, -0.5, 1.0, PURE_LIGHT));
		assertEquals(1, index.size());
	}

	@Test
	void removalAndChunkEvictionDiscardStalePositions() {
		LivingForceIndex index = new LivingForceIndex();
		long first = BlockPos.asLong(1, 64, 1);
		long second = BlockPos.asLong(17, 64, 1);
		index.add(first, DARKNESS);
		index.add(second, DARKNESS);

		index.remove(first);
		assertEquals(1, index.size());
		index.removeChunk(ChunkPos.pack(1, 0));
		assertEquals(0, index.size());
	}
}

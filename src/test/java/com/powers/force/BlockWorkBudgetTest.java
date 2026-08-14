package com.powers.force;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BlockWorkBudgetTest {
	@Test
	void dividesOneHardCapAcrossDimensionsForOneProtectionPolicySnapshot() {
		var overworld = new BlockWorkBudget.Lane("minecraft:overworld", 11L);
		var nether = new BlockWorkBudget.Lane("minecraft:the_nether", 11L);
		var end = new BlockWorkBudget.Lane("minecraft:the_end", 11L);

		var allocations = BlockWorkBudget.allocate(256,
				List.of(overworld, nether, end), 0L);

		assertEquals(256, allocations.values().stream().mapToInt(Integer::intValue).sum());
		assertTrue(allocations.values().stream().allMatch(value -> value >= 85 && value <= 86));
	}

	@Test
	void rotatesRemainderWithoutDependingOnInputOrderOrDuplicates() {
		var first = new BlockWorkBudget.Lane("a", 1L);
		var second = new BlockWorkBudget.Lane("b", 1L);
		var third = new BlockWorkBudget.Lane("c", 1L);

		var tickZero = BlockWorkBudget.allocate(2,
				List.of(third, first, second, first), 0L);
		var tickOne = BlockWorkBudget.allocate(2,
				List.of(third, first, second, first), 1L);

		assertEquals(List.of(first, second, third), tickZero.keySet().stream().sorted().toList());
		assertEquals(1, tickZero.get(first));
		assertEquals(0, tickZero.get(third));
		assertEquals(0, tickOne.get(second));
		assertEquals(1, tickOne.get(third));
	}

	@Test
	void zeroCapacityNeverManufacturesWork() {
		var lane = new BlockWorkBudget.Lane("minecraft:overworld", 0L);
		assertEquals(0, BlockWorkBudget.allocate(0, List.of(lane), 4L).get(lane));
		assertTrue(BlockWorkBudget.allocate(10, List.of(), 4L).isEmpty());
	}

	@Test
	void moreLanesThanCapacityReceiveWorkWithinOneRound() {
		int capacity = 256;
		int laneCount = 10_000;
		List<BlockWorkBudget.Lane> lanes = new ArrayList<>(laneCount);
		for (int index = 0; index < laneCount; index++) {
			lanes.add(new BlockWorkBudget.Lane("test:" + index, 7L));
		}
		Set<BlockWorkBudget.Lane> served = new HashSet<>();
		int roundTicks = (laneCount + capacity - 1) / capacity;
		for (int tick = 0; tick < roundTicks; tick++) {
			BlockWorkBudget.allocate(capacity, lanes, tick).forEach((lane, allowance) -> {
				if (allowance > 0) served.add(lane);
			});
		}

		assertEquals(laneCount, served.size(),
				"A capacity-sized cursor step must cover every lane within one round");
	}
}

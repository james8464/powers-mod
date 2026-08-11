package com.powers.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FairOwnerWorkQueueTest {
	private record Work(UUID owner, int value) { }

	@Test
	void oneOwnerCannotStarveAnotherInsideTheGlobalBudget() {
		FairOwnerWorkQueue<Work> queue = new FairOwnerWorkQueue<>(5, Work::owner);
		UUID loud = new UUID(0L, 1L);
		UUID quiet = new UUID(0L, 2L);
		for (int value = 0; value < 4; value++) assertTrue(queue.offer(new Work(loud, value)));
		assertTrue(queue.offer(new Work(quiet, 9)));

		assertEquals(List.of(new Work(loud, 0), new Work(quiet, 9), new Work(loud, 1)),
				queue.pollBatch(3));
	}

	@Test
	void capacityAndOwnerRemovalAreExact() {
		FairOwnerWorkQueue<Work> queue = new FairOwnerWorkQueue<>(2, Work::owner);
		UUID first = new UUID(0L, 1L);
		UUID second = new UUID(0L, 2L);
		assertTrue(queue.offer(new Work(first, 1)));
		assertTrue(queue.offer(new Work(second, 2)));
		assertFalse(queue.offer(new Work(first, 3)));
		assertEquals(1, queue.removeOwner(first));
		assertEquals(List.of(new Work(second, 2)), queue.pollBatch(2));
		assertEquals(0, queue.size());
	}
}

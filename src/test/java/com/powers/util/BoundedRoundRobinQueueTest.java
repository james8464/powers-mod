package com.powers.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoundedRoundRobinQueueTest {
	@Test
	void pollingIsBoundedStableAndReofferRotatesWork() {
		BoundedRoundRobinQueue<String> queue = new BoundedRoundRobinQueue<>();
		queue.offer("one");
		queue.offer("two");
		queue.offer("three");

		List<String> first = queue.pollBatch(2);
		assertEquals(List.of("one", "two"), first);
		first.forEach(queue::offer);
		assertEquals(List.of("three", "one"), queue.pollBatch(2));
	}

	@Test
	void duplicateOffersAndRemovalCannotCreateDuplicateWork() {
		BoundedRoundRobinQueue<String> queue = new BoundedRoundRobinQueue<>();
		queue.offer("one");
		queue.offer("one");
		queue.offer("two");
		queue.remove("one");

		assertEquals(List.of("two"), queue.pollBatch(20));
		assertEquals(List.of(), queue.pollBatch(20));
	}
}

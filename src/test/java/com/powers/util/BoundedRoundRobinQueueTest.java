package com.powers.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

	@Test
	void singlePollSupportsBoundedReofferWithoutScanningTheQueue() {
		BoundedRoundRobinQueue<String> queue = new BoundedRoundRobinQueue<>();
		queue.offer("one");
		queue.offer("two");

		assertEquals("one", queue.poll());
		queue.offer("one");
		assertEquals("two", queue.poll());
		assertEquals("one", queue.poll());
		assertEquals(null, queue.poll());
	}

	@Test
	void taskCountAboveCapacityStillCompletesOneRoundWithinTheBound() {
		int taskCount = 10_000;
		int capacity = 256;
		BoundedRoundRobinQueue<Integer> queue = new BoundedRoundRobinQueue<>();
		for (int task = 0; task < taskCount; task++) queue.offer(task);
		Set<Integer> served = new HashSet<>();
		int roundTicks = (taskCount + capacity - 1) / capacity;
		for (int tick = 0; tick < roundTicks; tick++) {
			for (int slot = 0; slot < capacity; slot++) {
				Integer task = queue.poll();
				if (task == null) break;
				served.add(task);
				queue.offer(task);
			}
		}

		assertEquals(taskCount, served.size(),
				"Every recurring task must receive a slot within ceil(tasks/capacity) ticks");
	}
}

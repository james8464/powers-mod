package com.powers.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduledTaskQueueTest {
	@Test
	void ownedDescriptorsExposeStableIdentityAndCancelEveryLifecycleCallback() {
		ScheduledTaskQueue queue = new ScheduledTaskQueue(4, 4, ignored -> { });
		UUID subject = UUID.randomUUID();
		UUID cancellationOwner = UUID.randomUUID();
		var first = new ScheduledTaskQueue.TaskDescriptor(subject, "minecraft:overworld", 12L,
				cancellationOwner, "locator_reveal");
		var second = new ScheduledTaskQueue.TaskDescriptor(subject, "powers:light_realm", 18L,
				cancellationOwner, "locator_finish");

		queue.schedule(first, () -> { });
		queue.schedule(second, () -> { });

		assertEquals(List.of(first, second), queue.snapshot());
		assertEquals(2, queue.cancelOwner(cancellationOwner));
		assertTrue(queue.snapshot().isEmpty());
		assertEquals(0, queue.runDue(20L));
	}

	@Test
	void runsOnlyDueTasksInStableOrderAndAllowsSchedulingFromCallbacks() {
		ScheduledTaskQueue queue = new ScheduledTaskQueue();
		List<String> events = new ArrayList<>();
		queue.schedule(5, () -> {
			events.add("first");
			queue.schedule(7, () -> events.add("nested"));
		});
		queue.schedule(5, () -> events.add("second"));
		queue.runDue(4);
		assertEquals(List.of(), events);
		queue.runDue(5);
		assertEquals(List.of("first", "second"), events);
		queue.runDue(7);
		assertEquals(List.of("first", "second", "nested"), events);
	}

	@Test
	void enforcesCapacityAndPerTickExecutionBudgetWithStableSpillover() {
		ScheduledTaskQueue queue = new ScheduledTaskQueue(3, 2, ignored -> { });
		List<Integer> events = new ArrayList<>();
		assertTrue(queue.schedule(5, () -> events.add(1)).accepted());
		assertTrue(queue.schedule(5, () -> events.add(2)).accepted());
		assertTrue(queue.schedule(5, () -> events.add(3)).accepted());
		assertFalse(queue.schedule(5, () -> events.add(4)).accepted());

		assertEquals(2, queue.runDue(5));
		assertEquals(List.of(1, 2), events);
		assertEquals(1, queue.runDue(6));
		assertEquals(List.of(1, 2, 3), events);
	}

	@Test
	void cancellationFreesCapacityAndPreventsExecution() {
		ScheduledTaskQueue queue = new ScheduledTaskQueue(1, 1, ignored -> { });
		List<String> events = new ArrayList<>();
		ScheduledTaskQueue.TaskToken cancelled = queue.schedule(10, () -> events.add("cancelled"));

		assertTrue(cancelled.cancel());
		assertFalse(cancelled.cancel());
		assertTrue(queue.schedule(10, () -> events.add("replacement")).accepted());
		assertEquals(1, queue.runDue(10));
		assertEquals(List.of("replacement"), events);
	}

	@Test
	void isolatesCallbackExceptionsAndContinuesTheDueBatch() {
		List<Throwable> errors = new ArrayList<>();
		ScheduledTaskQueue queue = new ScheduledTaskQueue(4, 4, errors::add);
		List<String> events = new ArrayList<>();
		queue.schedule(1, () -> { throw new IllegalStateException("broken ritual"); });
		queue.schedule(1, () -> events.add("survived"));

		assertEquals(2, queue.runDue(1));
		assertEquals(List.of("survived"), events);
		assertEquals(1, errors.size());
		assertEquals("broken ritual", errors.getFirst().getMessage());
	}
}

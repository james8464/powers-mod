package com.powers.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScheduledTaskQueueTest {
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
}

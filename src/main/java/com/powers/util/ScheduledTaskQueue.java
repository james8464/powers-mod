package com.powers.util;

import java.util.Comparator;
import java.util.PriorityQueue;

/** Allocation-light stable deadline queue; callbacks may safely enqueue more work. */
public final class ScheduledTaskQueue {
	private record Entry(long tick, long sequence, Runnable action) {
	}

	private final PriorityQueue<Entry> tasks = new PriorityQueue<>(
			Comparator.comparingLong(Entry::tick).thenComparingLong(Entry::sequence));
	private long sequence;

	public void schedule(long tick, Runnable action) {
		tasks.add(new Entry(tick, sequence++, action));
	}

	public void runDue(long tick) {
		while (!tasks.isEmpty() && tasks.peek().tick() <= tick) {
			tasks.remove().action().run();
		}
	}

	public void clear() {
		tasks.clear();
	}
}

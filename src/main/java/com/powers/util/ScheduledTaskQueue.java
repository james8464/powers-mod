package com.powers.util;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.function.Consumer;

/** Stable bounded deadline queue; callbacks may enqueue work and cannot break later tasks. */
public final class ScheduledTaskQueue {
	private enum State { ACTIVE, CANCELLED, EXECUTED, REJECTED }

	private record Entry(long tick, long sequence, TaskDescriptor descriptor,
			Runnable action, TaskToken token) {
	}

	/** Stable lifecycle identity retained by a delayed callback instead of world objects. */
	public record TaskDescriptor(UUID subjectId, String dimensionId, long deadline,
			UUID cancellationOwner, String purpose) {
		public TaskDescriptor {
			Objects.requireNonNull(subjectId, "subjectId");
			dimensionId = Objects.requireNonNull(dimensionId, "dimensionId").trim();
			Objects.requireNonNull(cancellationOwner, "cancellationOwner");
			purpose = Objects.requireNonNull(purpose, "purpose").trim();
			if (dimensionId.isEmpty() || purpose.isEmpty() || deadline < 0L) {
				throw new IllegalArgumentException("Delayed task identity must be complete");
			}
		}
	}

	/** Handle used by lifecycle owners to cancel work without retaining its callback. */
	public static final class TaskToken {
		private final ScheduledTaskQueue owner;
		private State state;

		private TaskToken(ScheduledTaskQueue owner, State state) {
			this.owner = owner;
			this.state = state;
		}

		public boolean accepted() {
			return state != State.REJECTED;
		}

		/** Cancels active work exactly once and immediately frees queue capacity. */
		public boolean cancel() {
			return owner != null && owner.cancel(this);
		}
	}

	private final PriorityQueue<Entry> tasks = new PriorityQueue<>(
			Comparator.comparingLong(Entry::tick).thenComparingLong(Entry::sequence));
	private final int capacity;
	private final int maxExecutionsPerTick;
	private final Consumer<Throwable> errorSink;
	private long sequence;
	private int activeCount;

	public ScheduledTaskQueue() {
		this(8_192, 256, ignored -> { });
	}

	public ScheduledTaskQueue(int capacity, int maxExecutionsPerTick, Consumer<Throwable> errorSink) {
		this.capacity = Math.max(1, capacity);
		this.maxExecutionsPerTick = Math.max(1, maxExecutionsPerTick);
		this.errorSink = Objects.requireNonNull(errorSink, "errorSink");
	}

	/** Schedules work or returns a rejected token when the hard capacity is full. */
	public synchronized TaskToken schedule(long tick, Runnable action) {
		return schedule(tick, null, action);
	}

	/** Schedules work with inspectable stable ownership and cancellation identity. */
	public synchronized TaskToken schedule(TaskDescriptor descriptor, Runnable action) {
		Objects.requireNonNull(descriptor, "descriptor");
		return schedule(descriptor.deadline(), descriptor, action);
	}

	private TaskToken schedule(long tick, TaskDescriptor descriptor, Runnable action) {
		Objects.requireNonNull(action, "action");
		if (activeCount >= capacity) return new TaskToken(null, State.REJECTED);
		TaskToken token = new TaskToken(this, State.ACTIVE);
		tasks.add(new Entry(tick, sequence++, descriptor, action, token));
		activeCount++;
		return token;
	}

	/** Cancels every active task owned by one lifecycle principal. */
	public synchronized int cancelOwner(UUID cancellationOwner) {
		Objects.requireNonNull(cancellationOwner, "cancellationOwner");
		int cancelled = 0;
		for (Entry entry : tasks) {
			if (entry.token.state == State.ACTIVE && entry.descriptor != null
					&& cancellationOwner.equals(entry.descriptor.cancellationOwner())) {
				entry.token.state = State.CANCELLED;
				activeCount--;
				cancelled++;
			}
		}
		return cancelled;
	}

	/** Stable active-task metadata for diagnostics and lifecycle audit tests. */
	public synchronized List<TaskDescriptor> snapshot() {
		return tasks.stream()
				.filter(entry -> entry.token.state == State.ACTIVE && entry.descriptor != null)
				.sorted(Comparator.comparingLong(Entry::tick).thenComparingLong(Entry::sequence))
				.map(Entry::descriptor)
				.toList();
	}

	/** Runs at most the per-tick budget and returns the number of callbacks attempted. */
	public synchronized int runDue(long tick) {
		int executed = 0;
		while (executed < maxExecutionsPerTick && !tasks.isEmpty() && tasks.peek().tick() <= tick) {
			Entry entry = tasks.remove();
			if (entry.token.state != State.ACTIVE) continue;
			entry.token.state = State.EXECUTED;
			activeCount--;
			executed++;
			try {
				entry.action.run();
			} catch (Throwable failure) {
				try {
					errorSink.accept(failure);
				} catch (Throwable ignored) {
					// Error reporting is deliberately unable to break the scheduler.
				}
			}
		}
		return executed;
	}

	private synchronized boolean cancel(TaskToken token) {
		if (token.state != State.ACTIVE) return false;
		token.state = State.CANCELLED;
		activeCount--;
		return true;
	}

	public synchronized void clear() {
		for (Entry entry : tasks) {
			if (entry.token.state == State.ACTIVE) entry.token.state = State.CANCELLED;
		}
		tasks.clear();
		activeCount = 0;
	}

	public synchronized int size() {
		return activeCount;
	}
}

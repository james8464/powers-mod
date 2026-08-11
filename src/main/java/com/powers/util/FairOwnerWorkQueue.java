package com.powers.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/** Capacity-bounded work queue that serves one task per owner per rotation. */
public final class FairOwnerWorkQueue<T> {
	private final int capacity;
	private final Function<T, UUID> ownerOf;
	private final Map<UUID, ArrayDeque<T>> workByOwner = new HashMap<>();
	private final ArrayDeque<UUID> owners = new ArrayDeque<>();
	private int size;

	public FairOwnerWorkQueue(int capacity, Function<T, UUID> ownerOf) {
		if (capacity < 1) throw new IllegalArgumentException("Capacity must be positive");
		this.capacity = capacity;
		this.ownerOf = Objects.requireNonNull(ownerOf, "ownerOf");
	}

	public boolean offer(T work) {
		Objects.requireNonNull(work, "work");
		UUID owner = Objects.requireNonNull(ownerOf.apply(work), "work owner");
		if (size >= capacity) return false;
		ArrayDeque<T> queue = workByOwner.get(owner);
		if (queue == null) {
			queue = new ArrayDeque<>();
			workByOwner.put(owner, queue);
			owners.addLast(owner);
		}
		queue.addLast(work);
		size++;
		return true;
	}

	public List<T> pollBatch(int maximum) {
		if (maximum < 0) throw new IllegalArgumentException("Maximum work cannot be negative");
		List<T> result = new ArrayList<>(Math.min(maximum, size));
		while (result.size() < maximum && !owners.isEmpty()) {
			UUID owner = owners.removeFirst();
			ArrayDeque<T> queue = workByOwner.get(owner);
			if (queue == null || queue.isEmpty()) continue;
			result.add(queue.removeFirst());
			size--;
			if (queue.isEmpty()) workByOwner.remove(owner);
			else owners.addLast(owner);
		}
		return List.copyOf(result);
	}

	public int removeOwner(UUID owner) {
		ArrayDeque<T> removed = workByOwner.remove(owner);
		if (removed == null) return 0;
		owners.remove(owner);
		int count = removed.size();
		size -= count;
		return count;
	}

	public int removeOwner(UUID owner, java.util.function.Predicate<T> predicate) {
		ArrayDeque<T> queue = workByOwner.get(owner);
		if (queue == null) return 0;
		int before = queue.size();
		queue.removeIf(predicate);
		int removed = before - queue.size();
		size -= removed;
		if (queue.isEmpty()) {
			workByOwner.remove(owner);
			owners.remove(owner);
		}
		return removed;
	}

	public void clear() {
		workByOwner.clear();
		owners.clear();
		size = 0;
	}

	public int size() {
		return size;
	}
}

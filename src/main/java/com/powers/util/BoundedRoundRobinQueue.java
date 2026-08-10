package com.powers.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Stable de-duplicated queue used to cap recurring server work per tick. */
public final class BoundedRoundRobinQueue<T> {
	private final ArrayDeque<T> queue = new ArrayDeque<>();
	private final Set<T> present = new HashSet<>();

	/** Offers a key at the back only when it is not already awaiting work. */
	public boolean offer(T value) {
		Objects.requireNonNull(value, "value");
		if (!present.add(value)) return false;
		queue.addLast(value);
		return true;
	}

	/** Removes up to the requested number; callers re-offer live recurring work. */
	public List<T> pollBatch(int maximum) {
		if (maximum < 0) throw new IllegalArgumentException("Maximum work cannot be negative");
		List<T> result = new ArrayList<>(Math.min(maximum, queue.size()));
		while (result.size() < maximum && !queue.isEmpty()) {
			T value = queue.removeFirst();
			present.remove(value);
			result.add(value);
		}
		return List.copyOf(result);
	}

	public boolean remove(T value) {
		if (!present.remove(value)) return false;
		queue.remove(value);
		return true;
	}

	public void clear() {
		queue.clear();
		present.clear();
	}

	public int size() {
		return queue.size();
	}
}

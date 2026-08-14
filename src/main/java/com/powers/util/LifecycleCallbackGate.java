package com.powers.util;

import java.util.Objects;
import java.util.Optional;

/** Resolves asynchronous callbacks only inside the lifecycle epoch that created them. */
public final class LifecycleCallbackGate<T> {
	private T active;
	private long epoch;

	public synchronized long bind(T value) {
		Objects.requireNonNull(value, "value");
		if (active != value) {
			active = value;
			epoch++;
		}
		return epoch;
	}

	public synchronized Optional<T> resolve(long expectedEpoch) {
		return active != null && expectedEpoch == epoch ? Optional.of(active) : Optional.empty();
	}

	public synchronized boolean clear(T value) {
		if (active != value) return false;
		active = null;
		epoch++;
		return true;
	}
}

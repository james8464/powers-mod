package com.powers.mind;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** One terminal winner for simultaneous physical-body and avatar fatalities. */
public final class FatalResolutionGate {
	public enum Cause { BODY, AVATAR }

	private final AtomicReference<Cause> winner = new AtomicReference<>();

	public boolean claim(Cause cause) {
		return winner.compareAndSet(null, Objects.requireNonNull(cause, "cause"));
	}

	public Cause winner() {
		return winner.get();
	}
}

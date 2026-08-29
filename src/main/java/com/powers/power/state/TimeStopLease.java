package com.powers.power.state;

import com.powers.time.ControlTick;

import java.util.Objects;
import java.util.UUID;

/** Immutable identity and control-clock bounds for one POWERS-owned freeze. */
public record TimeStopLease(long token, UUID owner, TimeStopLeaseSource source,
		ControlTick acquiredAt, ControlTick deadline, UUID shadowBody,
		boolean externallySuperseded) {
	public TimeStopLease {
		if (token <= 0L) throw new IllegalArgumentException("Lease token must be positive");
		Objects.requireNonNull(owner, "owner");
		Objects.requireNonNull(source, "source");
		Objects.requireNonNull(acquiredAt, "acquiredAt");
		Objects.requireNonNull(deadline, "deadline");
		if (deadline.value() < acquiredAt.value()) {
			throw new IllegalArgumentException("Lease deadline precedes acquisition");
		}
		if (source == TimeStopLeaseSource.SHADOW && shadowBody == null) {
			throw new IllegalArgumentException("Shadow lease requires its body identity");
		}
		if (source != TimeStopLeaseSource.SHADOW && shadowBody != null) {
			throw new IllegalArgumentException("Only Shadow leases may carry a body identity");
		}
	}

	public boolean indefinite() {
		return deadline.value() == Long.MAX_VALUE;
	}

	public TimeStopLease externallySupersededCopy() {
		return externallySuperseded ? this : new TimeStopLease(token, owner, source,
				acquiredAt, deadline, shadowBody, true);
	}
}

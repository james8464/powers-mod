package com.powers.power.state;

import com.powers.time.ControlTick;

import java.util.OptionalLong;
import java.util.UUID;

/** Pure acquisition, expiry, supersession, and release policy for Time Stop leases. */
public final class TimeStopLeaseRules {
	private TimeStopLeaseRules() {
	}

	public static boolean mayAcquire(boolean alreadyOwned, boolean vanillaFrozen) {
		return !alreadyOwned && !vanillaFrozen;
	}

	public static TimeStopLease create(long token, UUID owner, TimeStopLeaseSource source,
			ControlTick acquiredAt, long durationTicks, UUID shadowBody) {
		if (durationTicks < 0L) throw new IllegalArgumentException("Lease duration cannot be negative");
		ControlTick deadline = durationTicks == Long.MAX_VALUE
				? ControlTick.at(Long.MAX_VALUE) : acquiredAt.plus(durationTicks);
		return new TimeStopLease(token, owner, source, acquiredAt, deadline, shadowBody, false);
	}

	public static boolean expired(TimeStopLease lease, ControlTick now) {
		return !lease.indefinite() && now.value() >= lease.deadline().value();
	}

	public static boolean matchesRelease(TimeStopLease lease, long token, UUID owner,
			TimeStopLeaseSource source) {
		return lease != null && lease.token() == token && lease.owner().equals(owner)
				&& lease.source() == source;
	}

	public static TimeStopLease externallySupersede(TimeStopLease lease) {
		return lease.externallySupersededCopy();
	}

	public static boolean shouldUnfreeze(TimeStopLease lease, boolean vanillaFrozen) {
		return vanillaFrozen && !lease.externallySuperseded();
	}

	public static OptionalLong nextToken(long previous) {
		if (previous < 0L) throw new IllegalArgumentException("Previous lease token cannot be negative");
		return previous == Long.MAX_VALUE ? OptionalLong.empty() : OptionalLong.of(previous + 1L);
	}
}

package com.powers.power.state;

import com.powers.time.ControlTick;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/** Bounded mutable holder that applies the pure lease rules for one server. */
final class TimeStopLeaseBook {
	record ReleaseDecision(boolean matched, boolean unfreeze, TimeStopLease lease) {
		private static ReleaseDecision ignored() {
			return new ReleaseDecision(false, false, null);
		}
	}

	private long lastToken;
	private TimeStopLease active;
	private boolean retirementPending;

	Optional<TimeStopLease> acquire(UUID owner, TimeStopLeaseSource source,
			ControlTick now, long durationTicks, UUID shadowBody, boolean vanillaFrozen) {
		if (!TimeStopLeaseRules.mayAcquire(active != null, vanillaFrozen)) return Optional.empty();
		OptionalLong next = TimeStopLeaseRules.nextToken(lastToken);
		if (next.isEmpty()) return Optional.empty();
		lastToken = next.getAsLong();
		active = TimeStopLeaseRules.create(lastToken, owner, source, now, durationTicks, shadowBody);
		return Optional.of(active);
	}

	Optional<TimeStopLease> active() {
		return Optional.ofNullable(active);
	}

	boolean observeExternalWrite(BooleanSupplier retireJournal) {
		if (active != null) {
			active = null;
			retirementPending = true;
		}
		if (!retirementPending) return true;
		if (!retireJournal.getAsBoolean()) return false;
		retirementPending = false;
		return true;
	}

	ReleaseDecision release(long token, UUID owner, TimeStopLeaseSource source,
			boolean vanillaFrozen) {
		if (!TimeStopLeaseRules.matchesRelease(active, token, owner, source)) {
			return ReleaseDecision.ignored();
		}
		TimeStopLease released = active;
		active = null;
		return new ReleaseDecision(true,
				TimeStopLeaseRules.shouldUnfreeze(released, vanillaFrozen), released);
	}
}

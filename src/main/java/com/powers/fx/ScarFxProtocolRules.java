package com.powers.fx;

/** Defines the exact compact wire and monotonic generation rules for visual scar delivery. */
public final class ScarFxProtocolRules {
	public static final int CREATE_OR_UPDATE = 0;
	public static final int REMOVE = 1;
	public static final int RESET_DIMENSION = 2;

	private ScarFxProtocolRules() {
	}

	/** Reports whether all eight wire fields fit the closed protocol bounds. */
	public static boolean validate(Wire wire) {
		if (wire == null) return false;
		if (wire.operation() == RESET_DIMENSION) {
			return wire.position() == 0 && wire.face() == 0 && wire.impact() == 0
					&& wire.material() == 0 && wire.visualSeed() == 0
					&& wire.generation() > 0 && wire.leaseTicks() == 1;
		}
		return (wire.operation() == CREATE_OR_UPDATE || wire.operation() == REMOVE)
				&& wire.face() >= 0 && wire.face() < 6
				&& wire.impact() >= 0 && wire.impact() < 5
				&& wire.material() >= 0 && wire.material() < 6
				&& wire.generation() > 0 && wire.leaseTicks() >= 1 && wire.leaseTicks() <= 1_200;
	}

	/** Compares two positive generations using strict unsigned ordering. */
	public static boolean newerUnsigned(long candidate, long current) {
		return candidate > 0 && current > 0 && Long.compareUnsigned(candidate, current) > 0;
	}

	/** Advances one generation or permanently disables admissions at positive-counter exhaustion. */
	public static GenerationTransition advanceGeneration(long current, int activeRecords,
			long serverEpoch, long connectionEpoch) {
		if (current <= 0 || activeRecords < 0 || serverEpoch < 0 || connectionEpoch < 0) {
			throw new IllegalArgumentException("invalid allocator state");
		}
		if (current == Long.MAX_VALUE) {
			return new GenerationTransition(
					GenerationAction.DISABLE_NEW_SCARS_UNTIL_SERVER_RESTART,
					0, false, true, false, serverEpoch, connectionEpoch);
		}
		return new GenerationTransition(GenerationAction.ADVANCE,
				current + 1, true, true, true, serverEpoch, connectionEpoch);
	}

	/** Keeps an exhausted allocator disabled while allowing existing expiry and remove work. */
	public static GenerationTransition advanceDisabledGeneration(
			GenerationTransition transition, int activeRecords) {
		if (transition == null || activeRecords < 0) throw new IllegalArgumentException("invalid state");
		return transition.action() == GenerationAction.DISABLE_NEW_SCARS_UNTIL_SERVER_RESTART
				? transition : advanceGeneration(transition.nextGeneration(), activeRecords,
						transition.serverEpoch(), transition.connectionEpoch());
	}

	/** Starts generation one only after a real restart has required a client connection reset. */
	public static RestartGeneration serverRestart(GenerationTransition previous,
			long serverEpoch, long connectionEpoch) {
		if (previous == null
				|| previous.action() != GenerationAction.DISABLE_NEW_SCARS_UNTIL_SERVER_RESTART
				|| serverEpoch <= previous.serverEpoch()
				|| connectionEpoch <= previous.connectionEpoch()) {
			throw new IllegalArgumentException("restart must advance exhausted server and connection epochs");
		}
		return new RestartGeneration(1, serverEpoch, connectionEpoch, true);
	}

	/** Derives the remaining bounded lease from an absolute server expiry without sharing clocks. */
	public static int remainingLease(long expiresAt, long now) {
		if (expiresAt < 0 || now < 0) throw new IllegalArgumentException("ticks cannot be negative");
		if (expiresAt <= now) return 1;
		if (expiresAt - now > 1_200L) return 1_200;
		return (int) (expiresAt - now);
	}

	public enum GenerationAction { ADVANCE, DISABLE_NEW_SCARS_UNTIL_SERVER_RESTART }

	public record Wire(int operation, long position, int face, int impact,
			int material, int visualSeed, long generation, int leaseTicks) {
	}

	/** Returns the canonical fixed-field reset barrier for one delivery generation. */
	public static Wire resetDimension(long deliveryGeneration) {
		if (deliveryGeneration <= 0) throw new IllegalArgumentException("invalid delivery generation");
		return new Wire(RESET_DIMENSION, 0, 0, 0, 0, 0, deliveryGeneration, 1);
	}

	public record GenerationTransition(GenerationAction action, long nextGeneration,
			boolean newAdmissionsAllowed, boolean existingExpiryAndRemovesAllowed,
			boolean hasNextGeneration, long serverEpoch, long connectionEpoch) {
		public GenerationTransition {
			if (action == null || !existingExpiryAndRemovesAllowed
					|| serverEpoch < 0 || connectionEpoch < 0) {
				throw new IllegalArgumentException("invalid generation transition");
			}
			boolean disabled = action == GenerationAction.DISABLE_NEW_SCARS_UNTIL_SERVER_RESTART;
			if (disabled != (!newAdmissionsAllowed && !hasNextGeneration && nextGeneration == 0)
					|| !disabled && (!newAdmissionsAllowed || !hasNextGeneration || nextGeneration <= 0)) {
				throw new IllegalArgumentException("inconsistent generation transition");
			}
		}
	}

	public record RestartGeneration(long nextGeneration, long serverEpoch, long connectionEpoch,
			boolean clientConnectionResetRequired) {
		public RestartGeneration {
			if (nextGeneration != 1 || serverEpoch < 0 || connectionEpoch < 0
					|| !clientConnectionResetRequired) {
				throw new IllegalArgumentException("invalid restarted allocator state");
			}
		}
	}
}

package com.powers.animation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/** Pure bounded state owner for current server-authored casting poses. */
public final class CastingPoseLedger {
	public static final int MAX_ACTIVE = 256;
	public static final int MAX_IDENTITIES = 256;
	public static final int MAX_ATTEMPTS_PER_TICK = 64;
	public static final int RESERVED_TERMINAL_ATTEMPTS_PER_TICK = 8;
	public static final int MAX_START_ATTEMPTS_PER_TICK =
			MAX_ATTEMPTS_PER_TICK - RESERVED_TERMINAL_ATTEMPTS_PER_TICK;
	/** Compatibility alias; the bound now covers all start and terminal attempts. */
	public static final int MAX_OFFERS_PER_TICK = MAX_ATTEMPTS_PER_TICK;
	private static final String LEGACY_DIMENSION = "legacy";

	private final Map<UUID, IdentityState> identities = new HashMap<>();
	private long budgetTick = Long.MIN_VALUE;
	private int startAttemptsThisTick;
	private int terminalAttemptsThisTick;
	private long accepted;
	private long rejectedTickBudget;
	private long rejectedCapacity;
	private long rejectedSequence;
	private long rejectedSameEntityTick;

	public Optional<CastingPoseEvent> offer(int entityId, UUID entityUuid, CastingPose pose,
			CastingStyle style, CastingHand hand, long tick, int durationTicks) {
		return offer(entityId, entityUuid, LEGACY_DIMENSION, pose, style, hand, tick, durationTicks);
	}

	public Optional<CastingPoseEvent> offer(int entityId, UUID entityUuid, String dimension,
			CastingPose pose, CastingStyle style, CastingHand hand, long tick, int durationTicks) {
		Objects.requireNonNull(entityUuid, "entityUuid");
		dimension = requireDimension(dimension);
		rollBudget(tick);
		if (startAttemptsThisTick >= MAX_START_ATTEMPTS_PER_TICK) {
			rejectedTickBudget++;
			return Optional.empty();
		}
		startAttemptsThisTick++;
		IdentityState state = identities.get(entityUuid);
		if (state != null && !state.dimension().equals(dimension)) {
			identities.remove(entityUuid);
			state = null;
		}
		if (state != null && state.lastStartTick() == tick) {
			rejectedSameEntityTick++;
			return Optional.empty();
		}
		if (state == null && identities.size() >= MAX_IDENTITIES) {
			rejectedCapacity++;
			return Optional.empty();
		}
		long previous = state == null ? 0L : state.sequence();
		OptionalLong next = nextSequence(previous);
		if (next.isEmpty()) {
			rejectedSequence++;
			return Optional.empty();
		}
		CastingPoseEvent event = new CastingPoseEvent(entityId, entityUuid, next.getAsLong(), pose,
				style, hand, tick, durationTicks, false);
		identities.put(entityUuid, new IdentityState(dimension, event.sequence(), tick, event));
		accepted++;
		return Optional.of(event);
	}

	/** Creates an explicit ordered terminal packet and removes the active channel. */
	public Optional<CastingPoseEvent> terminate(int entityId, UUID entityUuid, String dimension,
			long tick) {
		Objects.requireNonNull(entityUuid, "entityUuid");
		dimension = requireDimension(dimension);
		rollBudget(tick);
		if (startAttemptsThisTick + terminalAttemptsThisTick >= MAX_ATTEMPTS_PER_TICK) {
			rejectedTickBudget++;
			return Optional.empty();
		}
		terminalAttemptsThisTick++;
		IdentityState state = identities.get(entityUuid);
		if (state == null || !state.dimension().equals(dimension) || state.active() == null) {
			return Optional.empty();
		}
		OptionalLong next = nextSequence(state.sequence());
		if (next.isEmpty()) {
			rejectedSequence++;
			return Optional.empty();
		}
		CastingPoseEvent current = state.active();
		CastingPoseEvent terminal = new CastingPoseEvent(entityId, entityUuid, next.getAsLong(),
				current.pose(), current.style(), current.hand(), tick, 1, true);
		identities.put(entityUuid, new IdentityState(dimension, terminal.sequence(),
				state.lastStartTick(), null));
		accepted++;
		return Optional.of(terminal);
	}

	public Optional<CastingPoseEvent> snapshot(UUID entityUuid, long tick) {
		return snapshot(identities.get(entityUuid), tick);
	}

	public Optional<CastingPoseEvent> snapshot(UUID entityUuid, String dimension, long tick) {
		IdentityState state = identities.get(entityUuid);
		return state != null && state.dimension().equals(requireDimension(dimension))
				? snapshot(state, tick) : Optional.empty();
	}

	public void tick(long tick, Predicate<UUID> entityAlive) {
		tick(tick, (uuid, dimension) -> entityAlive.test(uuid));
	}

	public void tick(long tick, BiPredicate<UUID, String> entityAliveInDimension) {
		identities.entrySet().removeIf(entry -> !entityAliveInDimension.test(entry.getKey(),
				entry.getValue().dimension()));
		identities.replaceAll((uuid, state) -> state.active() != null
				&& !CastingPoseRules.active(tick, state.active())
				? new IdentityState(state.dimension(), state.sequence(), state.lastStartTick(), null)
				: state);
	}

	public static OptionalLong nextSequence(long previous) {
		if (previous < 0 || previous == Long.MAX_VALUE) return OptionalLong.empty();
		return OptionalLong.of(previous + 1L);
	}

	public Metrics metrics() {
		int active = (int) identities.values().stream().filter(state -> state.active() != null).count();
		return new Metrics(accepted, rejectedTickBudget, rejectedCapacity, rejectedSequence,
				rejectedSameEntityTick, active, identities.size(), startAttemptsThisTick,
				terminalAttemptsThisTick);
	}

	public void clear() {
		identities.clear();
		budgetTick = Long.MIN_VALUE;
		startAttemptsThisTick = 0;
		terminalAttemptsThisTick = 0;
		accepted = 0;
		rejectedTickBudget = 0;
		rejectedCapacity = 0;
		rejectedSequence = 0;
		rejectedSameEntityTick = 0;
	}

	public void clear(UUID entityUuid) {
		IdentityState state = identities.get(entityUuid);
		if (state != null) identities.put(entityUuid, new IdentityState(state.dimension(),
				state.sequence(), state.lastStartTick(), null));
	}

	private static Optional<CastingPoseEvent> snapshot(IdentityState state, long tick) {
		CastingPoseEvent event = state == null ? null : state.active();
		return event != null && CastingPoseRules.active(tick, event)
				? Optional.of(event) : Optional.empty();
	}

	private void rollBudget(long tick) {
		if (budgetTick == tick) return;
		budgetTick = tick;
		startAttemptsThisTick = 0;
		terminalAttemptsThisTick = 0;
	}

	private static String requireDimension(String dimension) {
		if (dimension == null || dimension.isBlank()) throw new IllegalArgumentException("dimension");
		return dimension;
	}

	private record IdentityState(String dimension, long sequence, long lastStartTick,
			CastingPoseEvent active) {
	}

	public record Metrics(long accepted, long rejectedTickBudget, long rejectedCapacity,
			long rejectedSequence, long rejectedSameEntityTick, int activeEntries,
			int identityEntries, int startAttemptsThisTick, int terminalAttemptsThisTick) {
		public int attemptsThisTick() {
			return startAttemptsThisTick + terminalAttemptsThisTick;
		}

		public int offeredThisTick() {
			return attemptsThisTick();
		}
	}
}

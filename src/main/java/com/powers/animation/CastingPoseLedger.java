package com.powers.animation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.Predicate;

/** Pure bounded state owner for current server-authored casting poses. */
public final class CastingPoseLedger {
	public static final int MAX_ACTIVE = 256;
	public static final int MAX_OFFERS_PER_TICK = 64;

	private final Map<UUID, CastingPoseEvent> active = new HashMap<>();
	private final Map<UUID, Long> sequences = new HashMap<>();
	private final Map<UUID, Long> lastOfferedTick = new HashMap<>();
	private long budgetTick = Long.MIN_VALUE;
	private int offeredThisTick;
	private long accepted;
	private long rejectedTickBudget;
	private long rejectedCapacity;
	private long rejectedSequence;

	public Optional<CastingPoseEvent> offer(int entityId, UUID entityUuid, CastingPose pose,
			CastingStyle style, CastingHand hand, long tick, int durationTicks) {
		rollBudget(tick);
		active.entrySet().removeIf(entry -> !CastingPoseRules.active(tick, entry.getValue()));
		boolean sameEntityTick = lastOfferedTick.getOrDefault(entityUuid, Long.MIN_VALUE) == tick;
		if (!sameEntityTick && offeredThisTick >= MAX_OFFERS_PER_TICK) {
			rejectedTickBudget++;
			return Optional.empty();
		}
		if (!active.containsKey(entityUuid) && active.size() >= MAX_ACTIVE) {
			rejectedCapacity++;
			return Optional.empty();
		}
		OptionalLong next = nextSequence(sequences.getOrDefault(entityUuid, 0L));
		if (next.isEmpty()) {
			rejectedSequence++;
			return Optional.empty();
		}
		long sequence = next.getAsLong();
		CastingPoseEvent event = new CastingPoseEvent(entityId, entityUuid, sequence, pose, style,
				hand, tick, durationTicks);
		sequences.put(entityUuid, sequence);
		active.put(entityUuid, event);
		if (!sameEntityTick) {
			offeredThisTick++;
			lastOfferedTick.put(entityUuid, tick);
		}
		accepted++;
		return Optional.of(event);
	}

	public Optional<CastingPoseEvent> snapshot(UUID entityUuid, long tick) {
		CastingPoseEvent event = active.get(entityUuid);
		return event != null && CastingPoseRules.active(tick, event) ? Optional.of(event) : Optional.empty();
	}

	public void tick(long tick, Predicate<UUID> entityAlive) {
		active.entrySet().removeIf(entry -> !CastingPoseRules.active(tick, entry.getValue())
				|| !entityAlive.test(entry.getKey()));
		sequences.keySet().removeIf(uuid -> !entityAlive.test(uuid));
		lastOfferedTick.keySet().removeIf(uuid -> !entityAlive.test(uuid));
	}

	public static OptionalLong nextSequence(long previous) {
		if (previous < 0 || previous == Long.MAX_VALUE) return OptionalLong.empty();
		return OptionalLong.of(previous + 1L);
	}

	public Metrics metrics() {
		return new Metrics(accepted, rejectedTickBudget, rejectedCapacity, rejectedSequence,
				active.size(), offeredThisTick);
	}

	public void clear() {
		active.clear();
		sequences.clear();
		lastOfferedTick.clear();
		budgetTick = Long.MIN_VALUE;
		offeredThisTick = 0;
	}

	private void rollBudget(long tick) {
		if (budgetTick == tick) return;
		budgetTick = tick;
		offeredThisTick = 0;
	}

	public record Metrics(long accepted, long rejectedTickBudget, long rejectedCapacity,
			long rejectedSequence, int activeEntries, int offeredThisTick) {
	}
}

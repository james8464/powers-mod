package com.powers.magic.runtime;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Bounded server-thread history of recent ray capsules. It stores no world or
 * entity references and therefore cannot force chunks or retain players.
 */
public final class MagicRayCollisionIndex {
	public record Collision(MagicRaySegment submitted, MagicRaySegment existing, Vec3 point) {
	}

	private record PairKey(String firstOwner, String secondOwner, String firstAction, String secondAction) {
		private static PairKey of(MagicRaySegment first, MagicRaySegment second) {
			String ownerA = first.owner().toString();
			String ownerB = second.owner().toString();
			String actionA = first.action();
			String actionB = second.action();
			return new PairKey(ownerA.compareTo(ownerB) <= 0 ? ownerA : ownerB,
					ownerA.compareTo(ownerB) <= 0 ? ownerB : ownerA,
					actionA.compareTo(actionB) <= 0 ? actionA : actionB,
					actionA.compareTo(actionB) <= 0 ? actionB : actionA);
		}
	}

	private static final int PAIR_COOLDOWN_TICKS = 10;
	private final Map<String, ArrayDeque<MagicRaySegment>> byDimension = new HashMap<>();
	private final Map<PairKey, Long> lastCollision = new HashMap<>();
	private long budgetTick = Long.MIN_VALUE;
	private int collisionsThisTick;
	private final Map<UUID, Integer> ownerCollisionsThisTick = new HashMap<>();

	/** Stores one segment and returns its first admissible physical collision. */
	public Optional<Collision> submit(MagicRaySegment submitted) {
		if (submitted == null) return Optional.empty();
		long now = submitted.gameTime();
		resetBudget(now);
		ArrayDeque<MagicRaySegment> segments = byDimension.computeIfAbsent(
				submitted.dimension(), ignored -> new ArrayDeque<>());
		prune(segments, now);
		Optional<Collision> collision = Optional.empty();
		if (collisionsThisTick < MagicRayCollisionRules.MAX_COLLISIONS_PER_TICK
				&& hasOwnerBudget(submitted.owner())) {
			for (Iterator<MagicRaySegment> iterator = segments.descendingIterator(); iterator.hasNext();) {
				MagicRaySegment existing = iterator.next();
				if (!MagicRayCollisionRules.mayCompare(submitted, existing, now)) continue;
				if (!hasOwnerBudget(existing.owner())) continue;
				PairKey key = PairKey.of(submitted, existing);
				if (now - lastCollision.getOrDefault(key, Long.MIN_VALUE / 2) < PAIR_COOLDOWN_TICKS) continue;
				Optional<Vec3> point = MagicRayCollisionRules.intersection(submitted, existing);
				if (point.isEmpty()) continue;
				lastCollision.put(key, now);
				collisionsThisTick++;
				ownerCollisionsThisTick.merge(submitted.owner(), 1, Integer::sum);
				ownerCollisionsThisTick.merge(existing.owner(), 1, Integer::sum);
				collision = Optional.of(new Collision(submitted, existing, point.get()));
				break;
			}
		}
		segments.addLast(submitted);
		while (segments.size() > MagicRayCollisionRules.MAX_SEGMENTS_PER_DIMENSION) {
			segments.removeFirst();
		}
		return collision;
	}

	/** Expires old geometry and deduplication keys without inspecting entities. */
	public void tick(long now) {
		if (now < 0L) throw new IllegalArgumentException("Game time cannot be negative");
		resetBudget(now);
		byDimension.values().forEach(segments -> prune(segments, now));
		byDimension.values().removeIf(ArrayDeque::isEmpty);
		lastCollision.entrySet().removeIf(entry -> now - entry.getValue() >= PAIR_COOLDOWN_TICKS);
	}

	/** Clears only geometry owned by one departing or respawning caster. */
	public void clearOwner(UUID owner) {
		if (owner == null) return;
		byDimension.values().forEach(segments -> segments.removeIf(segment -> segment.owner().equals(owner)));
		byDimension.values().removeIf(ArrayDeque::isEmpty);
		String value = owner.toString();
		lastCollision.keySet().removeIf(key -> key.firstOwner().equals(value) || key.secondOwner().equals(value));
	}

	public int activeSegmentCount() {
		return byDimension.values().stream().mapToInt(ArrayDeque::size).sum();
	}

	public int collisionsThisTick() {
		return collisionsThisTick;
	}

	public void clear() {
		byDimension.clear();
		lastCollision.clear();
		budgetTick = Long.MIN_VALUE;
		collisionsThisTick = 0;
		ownerCollisionsThisTick.clear();
	}

	private void resetBudget(long now) {
		if (budgetTick == now) return;
		budgetTick = now;
		collisionsThisTick = 0;
		ownerCollisionsThisTick.clear();
	}

	private boolean hasOwnerBudget(UUID owner) {
		return ownerCollisionsThisTick.getOrDefault(owner, 0)
				< MagicRayCollisionRules.MAX_COLLISIONS_PER_OWNER_PER_TICK;
	}

	private static void prune(ArrayDeque<MagicRaySegment> segments, long now) {
		while (!segments.isEmpty()
				&& now - segments.peekFirst().gameTime() >= MagicRayCollisionRules.RETENTION_TICKS) {
			segments.removeFirst();
		}
	}
}

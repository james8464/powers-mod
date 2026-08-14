package com.powers.force;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Pure fair-share allocator for one hard block-inspection cap. */
public final class BlockWorkBudget {
	private BlockWorkBudget() {
	}

	/** A dimension and the bounded identity of the unanimous protection-provider policy. */
	public record Lane(String dimension, long providerPolicyId) implements Comparable<Lane> {
		public Lane {
			if (dimension == null || dimension.isBlank()) {
				throw new IllegalArgumentException("Dimension is required");
			}
		}

		@Override
		public int compareTo(Lane other) {
			int dimensionOrder = dimension.compareTo(Objects.requireNonNull(other).dimension);
			return dimensionOrder != 0 ? dimensionOrder
					: Long.compareUnsigned(providerPolicyId, other.providerPolicyId);
		}
	}

	/**
	 * Splits a fixed cap across unique lanes and rotates indivisible remainder work each tick.
	 * Every returned allowance is non-negative and their sum never exceeds {@code capacity}.
	 */
	public static Map<Lane, Integer> allocate(int capacity, Collection<Lane> active, long tick) {
		if (active == null || active.isEmpty()) return Map.of();
		List<Lane> lanes = active.stream().filter(Objects::nonNull).distinct().sorted().toList();
		if (lanes.isEmpty()) return Map.of();
		int boundedCapacity = Math.max(0, capacity);
		int base = boundedCapacity / lanes.size();
		int remainder = boundedCapacity % lanes.size();
		long tickInRound = Math.floorMod(tick, lanes.size());
		int remainderStart = (int) ((tickInRound * Math.max(1, remainder)) % lanes.size());
		Map<Lane, Integer> result = new LinkedHashMap<>();
		for (Lane lane : lanes) result.put(lane, base);
		for (int offset = 0; offset < remainder; offset++) {
			Lane lane = lanes.get((remainderStart + offset) % lanes.size());
			result.put(lane, result.get(lane) + 1);
		}
		return Map.copyOf(result);
	}
}

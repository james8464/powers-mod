package com.powers.network;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Per-tick visual-only deduplication by observer, dimension, chunk, action, and phase. */
public final class FxPacketCoalescer {
	private record Key(UUID observer, String dimension, int chunkX, int chunkZ,
			String action, String phase) { }

	/** Encoded payload-body traffic before and after visual-only coalescing. */
	public record TrafficSnapshot(long attemptedPackets, long deliveredPackets,
			long attemptedBytes, long deliveredBytes) {
		public double packetReductionPercent() {
			return reductionPercent(attemptedPackets, deliveredPackets);
		}

		public double byteReductionPercent() {
			return reductionPercent(attemptedBytes, deliveredBytes);
		}

		private static double reductionPercent(long attempted, long delivered) {
			if (attempted <= 0L) return 0.0;
			long boundedDelivered = Math.clamp(delivered, 0L, attempted);
			return (attempted - boundedDelivered) * 100.0 / attempted;
		}
	}

	private final int capacity;
	private final Set<Key> seen = new HashSet<>();
	private long tick = Long.MIN_VALUE;
	private long attemptedPackets;
	private long deliveredPackets;
	private long attemptedBytes;
	private long deliveredBytes;

	public FxPacketCoalescer(int capacity) {
		if (capacity < 1) throw new IllegalArgumentException("Capacity must be positive");
		this.capacity = capacity;
	}

	/** Returns false only for an exact repeated visual update in the current tick. */
	public boolean allow(long currentTick, UUID observer, String dimension,
			int chunkX, int chunkZ, String action, String phase, int encodedBytes) {
		if (encodedBytes < 0) throw new IllegalArgumentException("Encoded bytes cannot be negative");
		attemptedPackets = saturatedAdd(attemptedPackets, 1);
		attemptedBytes = saturatedAdd(attemptedBytes, encodedBytes);
		if (currentTick != tick) {
			tick = currentTick;
			seen.clear();
		}
		Key key = new Key(Objects.requireNonNull(observer), Objects.requireNonNull(dimension),
				chunkX, chunkZ, Objects.requireNonNull(action), Objects.requireNonNull(phase));
		if (seen.contains(key)) return false;
		if (seen.size() < capacity) seen.add(key);
		deliveredPackets = saturatedAdd(deliveredPackets, 1);
		deliveredBytes = saturatedAdd(deliveredBytes, encodedBytes);
		return true;
	}

	public TrafficSnapshot trafficSnapshot() {
		return new TrafficSnapshot(attemptedPackets, deliveredPackets,
				attemptedBytes, deliveredBytes);
	}

	public void clear() {
		seen.clear();
		tick = Long.MIN_VALUE;
		attemptedPackets = 0;
		deliveredPackets = 0;
		attemptedBytes = 0;
		deliveredBytes = 0;
	}

	private static long saturatedAdd(long current, long amount) {
		if (amount <= 0) return current;
		return Long.MAX_VALUE - current < amount ? Long.MAX_VALUE : current + amount;
	}
}

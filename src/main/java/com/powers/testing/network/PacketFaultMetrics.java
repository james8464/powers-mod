package com.powers.testing.network;

/** Identity-free aggregate counters for one active deterministic fault session. */
public record PacketFaultMetrics(long offered, long dropped, long duplicated, long delayed,
		long reordered, long delivered, long expired, long overflowed, long suppressedStale,
		long cancelled, long duplicateSideEffects, long maximumQueueDepth, long maximumAgeTicks) {
	public static PacketFaultMetrics empty() {
		return new PacketFaultMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
	}
}

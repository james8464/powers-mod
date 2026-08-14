package com.powers.client;

import com.powers.network.MagicFxPackets;
import java.util.ArrayDeque;
import java.util.List;

/** Bounded client-side transport evidence used by diagnostics and live acceptance tests. */
public final class ClientSemanticFxMetrics {
	private static final int MAX_RECENT_IDS = 256;
	private static final ArrayDeque<Long> RECENT_IDS = new ArrayDeque<>(MAX_RECENT_IDS);
	private static long individualPackets;
	private static long batchPackets;
	private static long batchedEntries;

	private ClientSemanticFxMetrics() {
	}

	public record Snapshot(long individualPackets, long batchPackets, long batchedEntries,
			List<Long> recentEventIds) {
		public Snapshot {
			recentEventIds = List.copyOf(recentEventIds);
		}
	}

	public static void recordIndividual(long eventId) {
		individualPackets++;
		append(eventId);
	}

	public static void recordBatch(List<MagicFxPackets.BatchEntry> entries) {
		batchPackets++;
		batchedEntries += entries.size();
		for (MagicFxPackets.BatchEntry entry : entries) append(eventId(entry));
	}

	public static Snapshot snapshot() {
		return new Snapshot(individualPackets, batchPackets, batchedEntries,
				List.copyOf(RECENT_IDS));
	}

	public static void reset() {
		individualPackets = 0;
		batchPackets = 0;
		batchedEntries = 0;
		RECENT_IDS.clear();
	}

	private static long eventId(MagicFxPackets.BatchEntry entry) {
		if (entry.magic() != null) return entry.magic().eventId();
		if (entry.beam() != null) return entry.beam().eventId();
		return entry.shape().eventId();
	}

	private static void append(long eventId) {
		if (RECENT_IDS.size() >= MAX_RECENT_IDS) RECENT_IDS.removeFirst();
		RECENT_IDS.addLast(eventId);
	}
}

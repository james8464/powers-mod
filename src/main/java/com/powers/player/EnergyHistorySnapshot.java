package com.powers.player;

import java.util.List;

/** Immutable bounded energy history with independently reconcilable totals. */
public record EnergyHistorySnapshot(long consumed, long restored, List<Breakdown> breakdown,
		List<Entry> history) {
	public EnergyHistorySnapshot {
		consumed = Math.max(0L, consumed);
		restored = Math.max(0L, restored);
		breakdown = List.copyOf(breakdown);
		history = List.copyOf(history);
	}

	public long amount(EnergyHistorySource source) {
		return breakdown.stream().filter(value -> value.source() == source)
				.mapToLong(Breakdown::amount).findFirst().orElse(0L);
	}

	public boolean reconciles() {
		long signed = history.stream().filter(entry -> entry.source().countsTowardUsage())
				.mapToLong(Entry::delta).sum();
		return signed == restored - consumed;
	}

	public String tooltip() {
		return "Energy history: spent=" + consumed + " restored=" + restored
				+ " net=" + (restored - consumed);
	}

	public record Breakdown(EnergyHistorySource source, long amount) { }
	public record Entry(long tick, EnergyHistorySource source, int before, int after) {
		public long delta() {
			return (long) after - before;
		}
	}
}

package com.powers.player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Session-owned bounded ledger fed only by completed authoritative mutations. */
public final class EnergyHistoryLedger {
	public static final int HISTORY_LIMIT = 32;
	private final Map<UUID, State> states = new HashMap<>();

	private static final class State {
		private long consumed;
		private long restored;
		private final EnumMap<EnergyHistorySource, Long> breakdown = new EnumMap<>(EnergyHistorySource.class);
		private final Deque<EnergyHistorySnapshot.Entry> history = new ArrayDeque<>();
	}

	public synchronized void record(UUID owner, long tick, EnergyHistorySource source, int before, int after) {
		if (owner == null || source == null || before == after) return;
		State state = states.computeIfAbsent(owner, ignored -> new State());
		long delta = (long) after - before;
		if (delta < 0) state.consumed = add(state.consumed, -delta);
		else state.restored = add(state.restored, delta);
		state.breakdown.merge(source, Math.abs(delta), EnergyHistoryLedger::add);
		state.history.addLast(new EnergyHistorySnapshot.Entry(Math.max(0L, tick), source, before, after));
		while (state.history.size() > HISTORY_LIMIT) state.history.removeFirst();
	}

	public synchronized EnergyHistorySnapshot snapshot(UUID owner) {
		State state = states.get(owner);
		if (state == null) state = new State();
		var breakdown = new ArrayList<EnergyHistorySnapshot.Breakdown>(EnergyHistorySource.values().length);
		for (EnergyHistorySource source : EnergyHistorySource.values()) {
			breakdown.add(new EnergyHistorySnapshot.Breakdown(source, state.breakdown.getOrDefault(source, 0L)));
		}
		return new EnergyHistorySnapshot(state.consumed, state.restored, breakdown,
				new ArrayList<>(state.history));
	}

	public synchronized void forget(UUID owner) {
		states.remove(owner);
	}

	public synchronized void clear() {
		states.clear();
	}

	private static long add(long left, long right) {
		return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
	}
}

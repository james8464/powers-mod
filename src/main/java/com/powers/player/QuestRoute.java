package com.powers.player;

import java.util.Map;

/** One authored, named alternative route to a cumulative progression milestone. */
public record QuestRoute<T extends Enum<T>>(String id, Map<T, Integer> thresholds) {
	public QuestRoute {
		if (id == null || !id.matches("[a-z0-9_]{1,48}") || thresholds == null || thresholds.isEmpty()
				|| thresholds.entrySet().stream().anyMatch(entry -> entry.getKey() == null
						|| entry.getValue() == null || entry.getValue() <= 0)) {
			throw new IllegalArgumentException("Invalid quest route");
		}
		thresholds = Map.copyOf(thresholds);
	}

	/** One event ledger is evaluated once; a level can therefore award only one route. */
	public boolean completed(Map<T, Integer> totals) {
		return thresholds.entrySet().stream()
				.allMatch(entry -> totals.getOrDefault(entry.getKey(), 0) >= entry.getValue());
	}
}

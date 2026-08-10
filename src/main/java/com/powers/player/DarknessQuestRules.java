package com.powers.player;

import java.util.List;
import java.util.Map;

/** Pure cumulative thresholds for the ten deliberately difficult shadow rites. */
public final class DarknessQuestRules {
	private static final List<Map<DarknessDeed, Integer>> RITES = List.of(
			Map.of(DarknessDeed.PASSIVE, 25),
			Map.of(DarknessDeed.PASSIVE, 100),
			Map.of(DarknessDeed.VILLAGER, 25, DarknessDeed.IRON_GOLEM, 5),
			Map.of(DarknessDeed.VILLAGER, 50, DarknessDeed.WOLF, 25,
					DarknessDeed.BABY_VILLAGER, 5),
			Map.of(DarknessDeed.VILLAGER, 75, DarknessDeed.WOLF, 50,
					DarknessDeed.BABY_VILLAGER, 10, DarknessDeed.IRON_GOLEM, 10),
			Map.of(DarknessDeed.VILLAGER, 125, DarknessDeed.WOLF, 100,
					DarknessDeed.BABY_VILLAGER, 20, DarknessDeed.IRON_GOLEM, 15),
			Map.of(DarknessDeed.VILLAGER, 200, DarknessDeed.WOLF, 175,
					DarknessDeed.BABY_VILLAGER, 35, DarknessDeed.IRON_GOLEM, 20),
			Map.of(DarknessDeed.VILLAGER, 300, DarknessDeed.WOLF, 250,
					DarknessDeed.BABY_VILLAGER, 50, DarknessDeed.IRON_GOLEM, 30),
			Map.of(DarknessDeed.VILLAGER, 400, DarknessDeed.WOLF, 375,
					DarknessDeed.BABY_VILLAGER, 75, DarknessDeed.IRON_GOLEM, 40),
			Map.of(DarknessDeed.VILLAGER, 500, DarknessDeed.WOLF, 500,
					DarknessDeed.BABY_VILLAGER, 100, DarknessDeed.IRON_GOLEM, 50));

	private DarknessQuestRules() {
	}

	/** Whether every cumulative threshold for {@code level} has been met. */
	public static boolean completed(int level, Map<DarknessDeed, Integer> deeds) {
		if (level < 1 || level > RITES.size()) {
			return false;
		}
		return RITES.get(level - 1).entrySet().stream()
				.allMatch(entry -> deeds.getOrDefault(entry.getKey(), 0) >= entry.getValue());
	}

	/** Advances through consecutive completed rites but can never skip a gap. */
	public static int highestContiguousLevel(int currentLevel, Map<DarknessDeed, Integer> deeds) {
		int level = Math.max(0, Math.min(currentLevel, RITES.size()));
		while (level < RITES.size() && completed(level + 1, deeds)) {
			level++;
		}
		return level;
	}
}

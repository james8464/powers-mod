package com.powers.player;

import java.util.List;
import java.util.Map;

/** Pure cumulative thresholds for the ten deliberately difficult shadow rites. */
public final class DarknessQuestRules {
	private static final List<List<QuestRoute<DarknessDeed>>> RITES = List.of(
			routes(Map.of(DarknessDeed.PASSIVE, 25), Map.of(DarknessDeed.VILLAGER, 8)),
			routes(Map.of(DarknessDeed.PASSIVE, 100),
					Map.of(DarknessDeed.WOLF, 35, DarknessDeed.IRON_GOLEM, 2)),
			routes(Map.of(DarknessDeed.VILLAGER, 50, DarknessDeed.IRON_GOLEM, 7),
					Map.of(DarknessDeed.PASSIVE, 250, DarknessDeed.WOLF, 40)),
			routes(Map.of(DarknessDeed.VILLAGER, 60, DarknessDeed.WOLF, 35,
					DarknessDeed.BABY_VILLAGER, 7),
					Map.of(DarknessDeed.PASSIVE, 400, DarknessDeed.IRON_GOLEM, 15)),
			routes(Map.of(DarknessDeed.VILLAGER, 75, DarknessDeed.WOLF, 50,
					DarknessDeed.BABY_VILLAGER, 10, DarknessDeed.IRON_GOLEM, 10),
					Map.of(DarknessDeed.VILLAGER, 130, DarknessDeed.IRON_GOLEM, 25)),
			routes(Map.of(DarknessDeed.VILLAGER, 125, DarknessDeed.WOLF, 100,
					DarknessDeed.BABY_VILLAGER, 20, DarknessDeed.IRON_GOLEM, 15),
					Map.of(DarknessDeed.PASSIVE, 800, DarknessDeed.WOLF, 250)),
			routes(Map.of(DarknessDeed.VILLAGER, 200, DarknessDeed.WOLF, 175,
					DarknessDeed.BABY_VILLAGER, 35, DarknessDeed.IRON_GOLEM, 20),
					Map.of(DarknessDeed.VILLAGER, 325, DarknessDeed.IRON_GOLEM, 55)),
			routes(Map.of(DarknessDeed.VILLAGER, 300, DarknessDeed.WOLF, 250,
					DarknessDeed.BABY_VILLAGER, 50, DarknessDeed.IRON_GOLEM, 30),
					Map.of(DarknessDeed.PASSIVE, 1_500, DarknessDeed.WOLF, 600)),
			routes(Map.of(DarknessDeed.VILLAGER, 400, DarknessDeed.WOLF, 375,
					DarknessDeed.BABY_VILLAGER, 75, DarknessDeed.IRON_GOLEM, 40),
					Map.of(DarknessDeed.VILLAGER, 650, DarknessDeed.IRON_GOLEM, 100)),
			routes(Map.of(DarknessDeed.VILLAGER, 500, DarknessDeed.WOLF, 500,
					DarknessDeed.BABY_VILLAGER, 100, DarknessDeed.IRON_GOLEM, 50),
					Map.of(DarknessDeed.PASSIVE, 3_000, DarknessDeed.WOLF, 1_200,
							DarknessDeed.IRON_GOLEM, 150)));

	private DarknessQuestRules() {
	}

	/** True when evaluation advanced beyond the level captured before rewards were applied. */
	public static boolean progressed(int previousLevel, int completedLevel) {
		return completedLevel > previousLevel;
	}

	/** Whether every cumulative threshold for {@code level} has been met. */
	public static boolean completed(int level, Map<DarknessDeed, Integer> deeds) {
		if (level < 1 || level > RITES.size()) {
			return false;
		}
		return completedRoute(level, deeds) != null;
	}

	/** Stable single winning route, even when a ledger satisfies both alternatives. */
	public static String completedRoute(int level, Map<DarknessDeed, Integer> deeds) {
		if (level < 1 || level > RITES.size()) return null;
		return RITES.get(level - 1).stream().filter(route -> route.completed(deeds))
				.map(QuestRoute::id).findFirst().orElse(null);
	}

	/** Immutable authored alternatives for UI, docs, and telemetry. */
	public static List<QuestRoute<DarknessDeed>> routes(int level) {
		return level < 1 || level > RITES.size() ? List.of() : RITES.get(level - 1);
	}

	private static List<QuestRoute<DarknessDeed>> routes(Map<DarknessDeed, Integer> atrocity,
			Map<DarknessDeed, Integer> predation) {
		return List.of(new QuestRoute<>("atrocity", atrocity), new QuestRoute<>("predation", predation));
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

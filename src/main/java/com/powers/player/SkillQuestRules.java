package com.powers.player;

import java.util.List;
import java.util.Map;

/** Cumulative long-form mastery requirements for the ten normal ranks. */
public final class SkillQuestRules {
	private static final List<Map<SkillDeed, Integer>> RITES = List.of(
			Map.of(SkillDeed.POWER_USE, 100, SkillDeed.POWER_KILL, 10),
			Map.of(SkillDeed.POWER_USE, 300, SkillDeed.POWER_KILL, 50),
			Map.of(SkillDeed.POWER_USE, 750, SkillDeed.POWER_KILL, 150),
			Map.of(SkillDeed.POWER_USE, 1500, SkillDeed.POWER_KILL, 300, SkillDeed.LIGHT_MEMORY, 1),
			Map.of(SkillDeed.POWER_USE, 2500, SkillDeed.POWER_KILL, 500,
					SkillDeed.LIGHT_MEMORY, 2, SkillDeed.BOSS_KILL, 1),
			Map.of(SkillDeed.POWER_USE, 4000, SkillDeed.POWER_KILL, 800,
					SkillDeed.LIGHT_MEMORY, 3, SkillDeed.BOSS_KILL, 2),
			Map.of(SkillDeed.POWER_USE, 6000, SkillDeed.POWER_KILL, 1200,
					SkillDeed.LIGHT_MEMORY, 4, SkillDeed.BOSS_KILL, 4),
			Map.of(SkillDeed.POWER_USE, 8500, SkillDeed.POWER_KILL, 1800,
					SkillDeed.LIGHT_MEMORY, 5, SkillDeed.BOSS_KILL, 7),
			Map.of(SkillDeed.POWER_USE, 12000, SkillDeed.POWER_KILL, 2500,
					SkillDeed.LIGHT_MEMORY, 6, SkillDeed.BOSS_KILL, 12),
			Map.of(SkillDeed.POWER_USE, 18000, SkillDeed.POWER_KILL, 4000,
					SkillDeed.LIGHT_MEMORY, 6, SkillDeed.BOSS_KILL, 25));

	private SkillQuestRules() {
	}

	public static boolean completed(int level, Map<SkillDeed, Integer> deeds) {
		if (level < 1 || level > RITES.size()) return false;
		return RITES.get(level - 1).entrySet().stream()
				.allMatch(entry -> deeds.getOrDefault(entry.getKey(), 0) >= entry.getValue());
	}

	public static int highestContiguousLevel(int currentLevel, Map<SkillDeed, Integer> deeds) {
		int level = Math.clamp(currentLevel, 0, RITES.size());
		while (level < RITES.size() && completed(level + 1, deeds)) level++;
		return level;
	}
}

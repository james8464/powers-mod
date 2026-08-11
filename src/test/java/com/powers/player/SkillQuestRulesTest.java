package com.powers.player;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillQuestRulesTest {
	@Test
	void firstRankRequiresPracticeAndPowerCombat() {
		assertFalse(SkillQuestRules.completed(1, Map.of(SkillDeed.POWER_USE, 100)));
		assertTrue(SkillQuestRules.completed(1,
				Map.of(SkillDeed.POWER_USE, 100, SkillDeed.POWER_KILL, 10)));
	}

	@Test
	void originRequiresLongTermMasteryMemoriesAndBosses() {
		EnumMap<SkillDeed, Integer> deeds = new EnumMap<>(SkillDeed.class);
		deeds.put(SkillDeed.POWER_USE, 18000);
		deeds.put(SkillDeed.POWER_KILL, 4000);
		deeds.put(SkillDeed.LIGHT_MEMORY, 6);
		deeds.put(SkillDeed.BOSS_KILL, 25);
		assertTrue(SkillQuestRules.completed(10, deeds));
		deeds.put(SkillDeed.BOSS_KILL, 24);
		assertFalse(SkillQuestRules.completed(10, deeds));
	}

	@Test
	void advancementRemainsStrictlyContiguous() {
		assertEquals(1, SkillQuestRules.highestContiguousLevel(0,
				Map.of(SkillDeed.POWER_USE, 299, SkillDeed.POWER_KILL, 49)));
	}

	@Test
	void everyRankOffersTwoStableAlternativesWithoutDoubleAwarding() {
		for (int level = 1; level <= 10; level++) {
			assertEquals(2, SkillQuestRules.routes(level).size());
		}
		Map<SkillDeed, Integer> overqualified = Map.of(
				SkillDeed.POWER_USE, 100_000,
				SkillDeed.POWER_KILL, 100_000,
				SkillDeed.LIGHT_MEMORY, 100,
				SkillDeed.BOSS_KILL, 100);
		assertEquals("mastery", SkillQuestRules.completedRoute(10, overqualified));
	}
}

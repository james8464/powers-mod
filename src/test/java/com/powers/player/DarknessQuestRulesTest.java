package com.powers.player;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DarknessQuestRulesTest {
	@Test
	void firstRiteRequiresTwentyFivePassiveVictims() {
		assertFalse(DarknessQuestRules.completed(1, Map.of(DarknessDeed.PASSIVE, 24)));
		assertTrue(DarknessQuestRules.completed(1, Map.of(DarknessDeed.PASSIVE, 25)));
	}

	@Test
	void nightfallRequiresEveryCapstoneAtrocity() {
		EnumMap<DarknessDeed, Integer> deeds = new EnumMap<>(DarknessDeed.class);
		deeds.put(DarknessDeed.VILLAGER, 500);
		deeds.put(DarknessDeed.WOLF, 500);
		deeds.put(DarknessDeed.BABY_VILLAGER, 100);
		deeds.put(DarknessDeed.IRON_GOLEM, 50);

		assertTrue(DarknessQuestRules.completed(10, deeds));
		deeds.put(DarknessDeed.WOLF, 499);
		assertFalse(DarknessQuestRules.completed(10, deeds));
	}

	@Test
	void highestNewlyCompletedLevelNeverSkipsAnUnmetRite() {
		Map<DarknessDeed, Integer> deeds = Map.of(DarknessDeed.PASSIVE, 100);

		assertEquals(2, DarknessQuestRules.highestContiguousLevel(0, deeds));
	}

	@Test
	void stricterThresholdsNeverRevokeAnAlreadyCompletedLevel() {
		assertEquals(7, DarknessQuestRules.highestContiguousLevel(7, Map.of()));
	}

	@Test
	void completionRefreshUsesTheLevelCapturedBeforeAwardsMutatePlayerState() {
		assertTrue(DarknessQuestRules.progressed(4, 5));
		assertFalse(DarknessQuestRules.progressed(5, 5));
	}

	@Test
	void everyRiteOffersTwoStableAlternativesWithoutDoubleAwarding() {
		for (int level = 1; level <= 10; level++) {
			assertEquals(2, DarknessQuestRules.routes(level).size());
		}
		Map<DarknessDeed, Integer> overqualified = Map.of(
				DarknessDeed.PASSIVE, 10_000,
				DarknessDeed.VILLAGER, 10_000,
				DarknessDeed.WOLF, 10_000,
				DarknessDeed.BABY_VILLAGER, 10_000,
				DarknessDeed.IRON_GOLEM, 10_000);
		assertEquals("atrocity", DarknessQuestRules.completedRoute(10, overqualified));
	}
}

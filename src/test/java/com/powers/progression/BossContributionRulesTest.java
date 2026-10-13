package com.powers.progression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossContributionRulesTest {
	@Test
	void onlyMeaningfulBoundedRecentActionsQualify() {
		assertFalse(BossContributionRules.qualifies(7.99, 0.0, 0));
		assertTrue(BossContributionRules.qualifies(8.0, 0.0, 0));
		assertFalse(BossContributionRules.qualifies(0.0, 5.99, 0));
		assertTrue(BossContributionRules.qualifies(0.0, 6.0, 0));
		assertFalse(BossContributionRules.qualifies(0.0, 0.0, 19));
		assertTrue(BossContributionRules.qualifies(0.0, 0.0, 20));
		assertTrue(BossContributionRules.recent(2_000, 3_200));
		assertFalse(BossContributionRules.recent(1_999, 3_200));
		assertFalse(BossContributionRules.validAmount(Double.NaN));
		assertFalse(BossContributionRules.validAmount(Double.POSITIVE_INFINITY));
	}
}

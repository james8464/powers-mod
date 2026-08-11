package com.powers.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcordCastRulesTest {
	@Test
	void onlyNearbyAlignedDistinctMatchingInnateCastsConcord() {
		assertTrue(ConcordCastRules.mayConcord(true, true, true, 8.0, 20, false));
		assertFalse(ConcordCastRules.mayConcord(false, true, true, 8.0, 20, false));
		assertFalse(ConcordCastRules.mayConcord(true, false, true, 8.0, 20, false));
		assertFalse(ConcordCastRules.mayConcord(true, true, false, 8.0, 20, false));
		assertFalse(ConcordCastRules.mayConcord(true, true, true, 13.0, 20, false));
		assertFalse(ConcordCastRules.mayConcord(true, true, true, 8.0, 41, false));
		assertFalse(ConcordCastRules.mayConcord(true, true, true, 8.0, 20, true));
	}

	@Test
	void pairCooldownAndWorkCapsAreExplicit() {
		assertTrue(ConcordCastRules.PAIR_COOLDOWN_TICKS >= 200);
		assertTrue(ConcordCastRules.MAX_RECENT_CASTS <= 512);
		assertTrue(ConcordCastRules.MAX_IMPACT_TARGETS <= 32);
	}
}

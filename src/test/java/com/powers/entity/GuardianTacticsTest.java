package com.powers.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuardianTacticsTest {
	@Test
	void transitionsBetweenMeleeRangedCoverAndRetreat() {
		assertEquals(GuardianTactics.Stance.MELEE,
				GuardianTactics.choose(2.0, 1.0, true, true));
		assertEquals(GuardianTactics.Stance.RANGED,
				GuardianTactics.choose(12.0, 1.0, true, true));
		assertEquals(GuardianTactics.Stance.SEEK_COVER,
				GuardianTactics.choose(12.0, 0.4, true, true));
		assertEquals(GuardianTactics.Stance.RETREAT,
				GuardianTactics.choose(3.0, 0.2, true, true));
		assertEquals(GuardianTactics.Stance.ADVANCE,
				GuardianTactics.choose(20.0, 1.0, false, true));
		assertEquals(GuardianTactics.Stance.IDLE,
				GuardianTactics.choose(5.0, 1.0, true, false));
	}

	@Test
	void navigationAndCoverSearchAreBoundedAndStaggered() {
		assertEquals(10, GuardianTactics.NAVIGATION_INTERVAL);
		assertEquals(20, GuardianTactics.COVER_SEARCH_INTERVAL);
		assertEquals(125, GuardianTactics.MAX_COVER_CANDIDATES);
		assertEquals(GuardianTactics.Stance.RANGED,
				GuardianTactics.choose(Double.NaN, Double.NaN, true, true));
	}
}

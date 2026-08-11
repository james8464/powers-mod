package com.powers.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionSyncRulesTest {
	@Test
	void sessionsAreDeterministicallyStaggeredAcrossTheUpdateInterval() {
		for (long session = 1; session <= 5; session++) {
			int updates = 0;
			for (int tick = 0; tick < 5; tick++) {
				if (CompanionSyncRules.shouldUpdate(tick, session)) updates++;
			}
			assertEquals(1, updates);
		}
		assertTrue(CompanionSyncRules.shouldUpdate(1, 1));
		assertFalse(CompanionSyncRules.shouldUpdate(2, 1));
	}

	@Test
	void viewerAllowanceIsBoundedAndShrinksWithSessionPressure() {
		assertEquals(128, CompanionSyncRules.viewerAllowance(1));
		assertEquals(128, CompanionSyncRules.viewerAllowance(100));
		assertEquals(40, CompanionSyncRules.viewerAllowance(500));
		assertEquals(8, CompanionSyncRules.viewerAllowance(10_000));
	}

	@Test
	void rotatingIndexWrapsWithoutOverflowOrNegativeValues() {
		assertEquals(2, CompanionSyncRules.rotatingIndex(7, 5));
		assertEquals(4, CompanionSyncRules.rotatingIndex(-1, 5));
		assertEquals(0, CompanionSyncRules.rotatingIndex(Long.MAX_VALUE, 1));
	}
}

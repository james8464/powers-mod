package com.powers.force;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionInvasionRulesTest {
	@Test
	void opposingForceInvadesButAlignedForceDoesNot() {
		assertTrue(FactionInvasionRules.shouldInvade(LivingForceKind.DARKNESS, false));
		assertFalse(FactionInvasionRules.shouldInvade(LivingForceKind.DARKNESS, true));
		assertTrue(FactionInvasionRules.shouldInvade(LivingForceKind.PURE_LIGHT, true));
		assertFalse(FactionInvasionRules.shouldInvade(LivingForceKind.PURE_LIGHT, false));
	}

	@Test
	void invasionAndScarWorkStayHardCapped() {
		assertEquals(64, FactionInvasionRules.GLOBAL_INVADER_CAP);
		assertEquals(3, FactionInvasionRules.NEARBY_INVADER_CAP);
		assertEquals(5, FactionInvasionRules.scarOffsets().size());
	}

	@Test
	void playerAnchorWindowsVisitEveryIndexWithoutExceedingThePerPulseCap() {
		assertCompleteRoundRobinCoverage(65);
		assertCompleteRoundRobinCoverage(100);
	}

	@Test
	void playerAnchorCursorHasAnExplicitLifecycleReset() {
		assertEquals(0, FactionInvasionRules.initialPlayerAnchorCursor());
	}

	private static void assertCompleteRoundRobinCoverage(int playerCount) {
		Set<Integer> visited = new HashSet<>();
		int cursor = FactionInvasionRules.initialPlayerAnchorCursor();
		for (int pulse = 0; pulse < 2; pulse++) {
			FactionInvasionRules.AnchorWindow window =
					FactionInvasionRules.playerAnchorWindow(playerCount, cursor);
			assertTrue(window.indexes().size() <= 64);
			visited.addAll(window.indexes());
			cursor = window.nextCursor();
		}
		assertEquals(playerCount, visited.size());
	}
}

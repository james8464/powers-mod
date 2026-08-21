package com.powers.force;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
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
		assertEquals(0, FactionInvasionRules.initialPlayerAnchor());
	}

	@Test
	void truncatedNearCapWindowResumesAfterTheLastActuallyVisitedAnchor() {
		FactionInvasionRules.AnchorWindow first =
				FactionInvasionRules.playerAnchorWindow(100, 0);
		int next = first.nextAnchorAfterVisited(1);
		FactionInvasionRules.AnchorWindow second =
				FactionInvasionRules.playerAnchorWindow(100, next);

		assertEquals(1, next);
		assertEquals(1, second.indexes().getFirst());
	}

	@Test
	void boundedCursorStillCoversRepresentativeJoinLeaveAndReorderChurn() {
		FactionInvasionRules.AnchorWindow first = FactionInvasionRules.playerAnchorWindow(5, 0);
		int next = first.nextAnchorAfterVisited(2);
		FactionInvasionRules.AnchorWindow afterReorderAndReplacement =
				FactionInvasionRules.playerAnchorWindow(5, next);
		Set<Integer> visited = new HashSet<>(afterReorderAndReplacement.indexes());

		assertEquals(Set.of(0, 1, 2, 3, 4), visited);
		assertTrue(afterReorderAndReplacement.indexes().size() <= 64);
	}

	private static void assertCompleteRoundRobinCoverage(int playerCount) {
		Set<Integer> visited = new HashSet<>();
		int next = FactionInvasionRules.initialPlayerAnchor();
		for (int pulse = 0; pulse < 2; pulse++) {
			FactionInvasionRules.AnchorWindow window =
					FactionInvasionRules.playerAnchorWindow(playerCount, next);
			assertTrue(window.indexes().size() <= 64);
			visited.addAll(window.indexes());
			next = window.nextAnchorAfterVisited(window.indexes().size());
		}
		assertEquals(playerCount, visited.size());
	}
}

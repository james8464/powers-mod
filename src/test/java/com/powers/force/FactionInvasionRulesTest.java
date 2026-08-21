package com.powers.force;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
	void truncatedNumericCursorStaysBoundedAndIndexValidAcrossRealIdentityChurn() {
		List<UUID> players = new ArrayList<>();
		for (int index = 0; index < 100; index++) players.add(new UUID(0L, index + 1L));
		UUID firstVisited = players.getFirst();
		UUID removed = players.get(20);
		UUID joined = new UUID(1L, 1L);
		FactionInvasionRules.AnchorWindow first = FactionInvasionRules.playerAnchorWindow(players.size(), 0);
		int next = first.nextAnchorAfterVisited(1);

		players.remove(removed);
		players.add(joined);
		players.remove(firstVisited);
		players.add(1, firstVisited);
		java.util.Collections.swap(players, 30, 70);
		FactionInvasionRules.AnchorWindow afterChurn =
				FactionInvasionRules.playerAnchorWindow(players.size(), next);
		Set<UUID> actualIdentities = new HashSet<>();
		for (int index : afterChurn.indexes()) actualIdentities.add(players.get(index));

		assertEquals(1, next);
		assertEquals(64, afterChurn.indexes().size());
		assertEquals(64, actualIdentities.size());
		assertFalse(players.contains(removed));
		assertTrue(players.contains(joined));
		assertTrue(afterChurn.indexes().stream().allMatch(index -> index >= 0 && index < players.size()));
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

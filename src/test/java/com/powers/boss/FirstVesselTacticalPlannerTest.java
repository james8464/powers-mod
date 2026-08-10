package com.powers.boss;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirstVesselTacticalPlannerTest {
	private final FirstVesselTacticalPlanner planner = new FirstVesselTacticalPlanner();

	@Test
	void rangeClusterLowHealthAndCoverChangeTheWinningTool() {
		var deck = FirstVesselPowerCatalogue.actions();
		var ranged = planner.choose(deck, facts(30, true, 1, 0, 0.9, false, false),
				1_000, Map.of());
		assertNotNull(ranged);
		assertTrue(ranged.action().kind() == FirstVesselPowerAction.Kind.BEAM
				|| ranged.action().kind() == FirstVesselPowerAction.Kind.PROJECTILE);

		var clustered = planner.choose(deck, facts(7, true, 7, 3, 0.9, true, false),
				1_000, Map.of());
		assertEquals(FirstVesselPowerAction.Kind.AREA, clustered.action().kind());

		var wounded = planner.choose(deck, facts(9, true, 1, 5, 0.25, false, false),
				1_000, Map.of());
		assertTrue(wounded.action().kind() == FirstVesselPowerAction.Kind.RECOVERY
				|| wounded.action().kind() == FirstVesselPowerAction.Kind.DEFENSE);

		var covered = planner.choose(deck, facts(24, false, 1, 0, 0.8, true, false),
				1_000, Map.of());
		assertEquals(FirstVesselPowerAction.Kind.MOBILITY, covered.action().kind());
	}

	@Test
	void cooldownsWardsRepetitionAndInvalidTargetsAreRespected() {
		var deck = FirstVesselPowerCatalogue.actions();
		var baseline = planner.choose(deck, facts(28, true, 1, 0, 0.8, true, false),
				1_000, Map.of());
		Map<String, Integer> cooldown = Map.of(baseline.action().powerId(), 1_000);
		var next = planner.choose(deck, facts(28, true, 1, 0, 0.8, true, false),
				1_000, cooldown);
		assertNotEquals(baseline.action().powerId(), next.action().powerId());

		var warded = planner.choose(deck, facts(28, true, 1, 0, 0.8, true, true),
				1_000, Map.of());
		assertTrue(warded.action().kind() == FirstVesselPowerAction.Kind.MOBILITY
				|| warded.action().kind() == FirstVesselPowerAction.Kind.DEFENSE);
		assertNull(planner.choose(deck, new FirstVesselEncounterFacts(false, 0, 0,
				false, 0, 0, 1.0, false, false, false, "none", 1), 1_000, Map.of()));
	}

	@Test
	void twentyAndFiftyPlayerSimulationsStayInsideTheCandidateBudget() {
		var deck = FirstVesselPowerCatalogue.actions();
		for (int players : List.of(20, 50)) {
			long inspected = 0;
			Map<String, Integer> cooldowns = new HashMap<>();
			for (int decision = 0; decision < players * 20; decision++) {
				var result = planner.choose(deck, facts(6 + decision % 35, true,
						Math.min(players, 8), decision % 6, 0.8, true, decision % 7 == 0),
						10_000 + decision * 10, cooldowns);
				assertNotNull(result);
				inspected += result.evaluatedCandidates();
				cooldowns.put(result.action().powerId(), 10_000 + decision * 10);
			}
			assertTrue(inspected <= (long) players * 20 * FirstVesselRules.MAX_CANDIDATES);
		}
	}

	private static FirstVesselEncounterFacts facts(double distance, boolean lineOfSight,
			int cluster, int projectiles, double health, boolean moving, boolean warded) {
		return new FirstVesselEncounterFacts(true, distance, 2.0, lineOfSight,
				cluster, projectiles, health, moving, warded, !lineOfSight, "none", 31L);
	}
}

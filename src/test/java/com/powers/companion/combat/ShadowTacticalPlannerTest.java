package com.powers.companion.combat;

import com.powers.power.PowerRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowTacticalPlannerTest {
	@BeforeAll static void initialize() { PowerRegistry.initialize(); }

	@Test
	void hardSafetyFactsChooseCloseFarRescueAndRecover() {
		var learner = new ShadowLearningState();
		assertEquals(ShadowEngagementMode.CLOSE, decide(facts(8, false, true, 1, 1, false), learner).mode());
		assertEquals(ShadowEngagementMode.FAR, decide(facts(6, true, false, 1, 1, false), learner).mode());
		assertEquals(ShadowEngagementMode.RESCUE, decide(facts(12, true, false, .2, 1, false), learner).mode());
		assertEquals(ShadowEngagementMode.RECOVER, decide(facts(12, true, false, 1, .1, true), learner).mode());
	}

	@Test
	void plannerEvaluatesOnlyTheBoundedLegalListAndAvoidsBlockedFiringLanes() {
		List<ShadowPowerAction> legal = ShadowPowerCatalogue.actions().subList(0, 7);
		var decision = ShadowTacticalPlanner.choose(legal,
				new ShadowCombatFacts(18, .8, .8, true, false, .9, .8, .8,
						false, true, ShadowRequestRange.AUTO), new ShadowLearningState());
		assertEquals(7, decision.evaluatedCount());
		assertNotNull(decision.action());
		assertTrue(decision.action().intent() != ShadowPowerAction.Intent.OFFENSE
				|| decision.action().range() != ShadowPowerAction.RangeMode.FAR);
	}

	private static ShadowTacticalPlanner.Decision decide(ShadowCombatFacts facts,
			ShadowLearningState learner) {
		return ShadowTacticalPlanner.choose(ShadowPowerCatalogue.actions(), facts, learner);
	}

	private static ShadowCombatFacts facts(double distance, boolean boss, boolean ranged,
			double ownerHealth, double energy, boolean suppressed) {
		return new ShadowCombatFacts(distance, ranged ? .5 : .8, boss ? 1 : .3, ranged, boss,
				ownerHealth, .9, energy, suppressed, false, ShadowRequestRange.AUTO);
	}
}

package com.powers.companion.combat;

import com.powers.power.PowerRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
	void encounterRolesAreDistinctInspectableAndSafetyOrdered() {
		var learner = new ShadowLearningState();
		assertEquals(ShadowEncounterRole.DISENGAGE, decide(facts(.8, .4, false, false,
				.9, .9, .8, true, false, false, ShadowRequestRange.AUTO), learner).role());
		assertEquals(ShadowEncounterRole.RESCUE, decide(facts(.8, .4, false, false,
				.2, .9, .8, false, false, false, ShadowRequestRange.AUTO), learner).role());
		assertEquals(ShadowEncounterRole.PEEL, decide(facts(.8, .8, false, false,
				.9, .9, .8, false, false, true, ShadowRequestRange.AUTO), learner).role());
		assertEquals(ShadowEncounterRole.INTERRUPT, decide(facts(.8, .3, true, false,
				.9, .9, .8, false, false, false, ShadowRequestRange.AUTO), learner).role());
		assertEquals(ShadowEncounterRole.EXECUTE, decide(facts(.15, .3, false, false,
				.9, .9, .8, false, false, false, ShadowRequestRange.AUTO), learner).role());
		assertEquals(ShadowEncounterRole.SUPPRESS, decide(facts(.8, .3, false, false,
				.9, .9, .8, false, false, false, ShadowRequestRange.AUTO), learner).role());
	}

	@Test
	void amethystAndFriendlyFireAreHardStopsBeforeRoleUtility() {
		List<ShadowPowerAction> actions = List.of(
				new ShadowPowerAction("far_offense", ShadowPowerAction.RangeMode.FAR,
						ShadowPowerAction.Intent.OFFENSE, 10, false,
						ShadowPowerAction.WorkClass.PROJECTILE, 1),
				new ShadowPowerAction("area", ShadowPowerAction.RangeMode.CLOSE,
						ShadowPowerAction.Intent.TERRAIN, 10, false,
						ShadowPowerAction.WorkClass.TERRAIN, 1),
				new ShadowPowerAction("control", ShadowPowerAction.RangeMode.MID,
						ShadowPowerAction.Intent.CONTROL, 0, false,
						ShadowPowerAction.WorkClass.CHEAP, 1));
		ShadowCombatFacts allyInLane = facts(.8, .3, false, false, .9, .9, .8,
				false, true, false, ShadowRequestRange.AUTO);
		var safe = ShadowTacticalPlanner.choose(actions, allyInLane, new ShadowLearningState());
		assertEquals("control", safe.action().id());
		assertEquals(2, safe.rejectedUnsafeCount());

		ShadowCombatFacts suppressed = facts(.8, .3, false, false, .9, .9, .8,
				true, false, false, ShadowRequestRange.AUTO);
		var stopped = ShadowTacticalPlanner.choose(actions, suppressed, new ShadowLearningState());
		assertEquals(ShadowEncounterRole.DISENGAGE, stopped.role());
		assertNull(stopped.action());
	}

	@Test
	void ownerRangePreferenceChangesRoleExecutionWithoutOverridingTheRole() {
		var learner = new ShadowLearningState();
		var close = decide(facts(.8, .3, false, false, .9, .9, .8,
				false, false, false, ShadowRequestRange.CLOSE), learner);
		var far = decide(facts(.8, .3, false, false, .9, .9, .8,
				false, false, false, ShadowRequestRange.FAR), learner);
		assertEquals(ShadowEncounterRole.SUPPRESS, close.role());
		assertEquals(ShadowEncounterRole.SUPPRESS, far.role());
		assertEquals(ShadowTacticalPlanner.Movement.APPROACH, close.movement());
		assertEquals(ShadowTacticalPlanner.Movement.RETREAT, far.movement());
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

	private static ShadowCombatFacts facts(double targetHealth, double meleeDanger,
			boolean ranged, boolean boss, double ownerHealth, double shadowHealth,
			double energy, boolean suppressed, boolean allyInLane, boolean ownerThreatened,
			ShadowRequestRange preference) {
		return new ShadowCombatFacts(8, targetHealth, meleeDanger, ranged, boss,
				ownerHealth, shadowHealth, energy, suppressed, allyInLane,
				ownerThreatened, preference);
	}
}

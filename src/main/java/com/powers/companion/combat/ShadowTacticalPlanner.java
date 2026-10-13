package com.powers.companion.combat;

import java.util.List;

/** Pure deterministic utility planner over at most the 26 already-legal actions. */
public final class ShadowTacticalPlanner {
	public enum Movement { APPROACH, ORBIT, RETREAT, INTERPOSE, RECOVER }
	public record Decision(ShadowEncounterRole role, ShadowEngagementMode mode,
			ShadowPowerAction action, Movement movement, double score,
			int evaluatedCount, int rejectedUnsafeCount) { }

	private ShadowTacticalPlanner() {
	}

	public static Decision choose(List<ShadowPowerAction> legal, ShadowCombatFacts facts,
			ShadowLearningState learning) {
		ShadowEncounterRole role = role(facts);
		ShadowEngagementMode mode = mode(facts);
		int boundedCount = Math.min(26, legal.size());
		if (facts.suppressed()) {
			return new Decision(role, mode, null, Movement.RECOVER, -Double.MAX_VALUE,
					boundedCount, boundedCount);
		}
		ShadowPowerAction best = null;
		double bestScore = -Double.MAX_VALUE;
		int evaluated = 0;
		int rejectedUnsafe = 0;
		String context = facts.contextKey(mode);
		String type = facts.archetype().name().toLowerCase();
		for (int index = 0; index < boundedCount; index++) {
			ShadowPowerAction action = legal.get(index);
			evaluated++;
			if (action.cost() > facts.energyRatio() * 1_850.0) continue;
			if (unsafeForAlly(action, facts)) {
				rejectedUnsafe++;
				continue;
			}
			double score = base(mode, action) + roleBonus(role, action);
			score *= 1.0 + learning.modifier(context, type, action.id());
			if (!facts.suppressed() && facts.ownerHealthRatio() >= .5
					&& facts.shadowHealthRatio() >= .5 && !facts.boss()) {
				score += learning.confidenceBonus(context, type, action.id());
			}
			if (score > bestScore) {
				bestScore = score;
				best = action;
			}
		}
		return new Decision(role, mode, best, movement(role, mode), bestScore,
				evaluated, rejectedUnsafe);
	}

	private static ShadowEncounterRole role(ShadowCombatFacts facts) {
		if (facts.suppressed() || facts.energyRatio() < .15 || facts.shadowHealthRatio() < .2) {
			return ShadowEncounterRole.DISENGAGE;
		}
		if (facts.ownerHealthRatio() < .3) return ShadowEncounterRole.RESCUE;
		if (facts.ownerThreatened()) return ShadowEncounterRole.PEEL;
		if (facts.targetRanged()) return ShadowEncounterRole.INTERRUPT;
		if (facts.targetHealthRatio() <= .25) return ShadowEncounterRole.EXECUTE;
		return ShadowEncounterRole.SUPPRESS;
	}

	private static boolean unsafeForAlly(ShadowPowerAction action, ShadowCombatFacts facts) {
		if (!facts.allyInFiringLane()) return false;
		return action.destructionTier() > 0
				|| action.intent() == ShadowPowerAction.Intent.OFFENSE
				|| action.intent() == ShadowPowerAction.Intent.TERRAIN
				|| action.intent() == ShadowPowerAction.Intent.SUMMON;
	}

	private static double roleBonus(ShadowEncounterRole role, ShadowPowerAction action) {
		return switch (role) {
			case PEEL -> intent(action, ShadowPowerAction.Intent.CONTROL, 8.0,
					ShadowPowerAction.Intent.DEFENSE, 5.0);
			case INTERRUPT -> intent(action, ShadowPowerAction.Intent.CONTROL, 8.0,
					ShadowPowerAction.Intent.OFFENSE, 3.0);
			case RESCUE -> intent(action, ShadowPowerAction.Intent.DEFENSE, 8.0,
					ShadowPowerAction.Intent.CONTROL, 6.0);
			case SUPPRESS -> intent(action, ShadowPowerAction.Intent.CONTROL, 7.0,
					ShadowPowerAction.Intent.DEFENSE, 2.0);
			case EXECUTE -> intent(action, ShadowPowerAction.Intent.OFFENSE, 8.0,
					ShadowPowerAction.Intent.CONTROL, 2.0);
			case DISENGAGE -> intent(action, ShadowPowerAction.Intent.RECOVERY, 8.0,
					ShadowPowerAction.Intent.MOBILITY, 6.0);
		};
	}

	private static double intent(ShadowPowerAction action, ShadowPowerAction.Intent first,
			double firstScore, ShadowPowerAction.Intent second, double secondScore) {
		if (action.intent() == first) return firstScore;
		if (action.intent() == second) return secondScore;
		return 0.0;
	}

	private static ShadowEngagementMode mode(ShadowCombatFacts facts) {
		if (facts.suppressed() || facts.energyRatio() < .15 || facts.shadowHealthRatio() < .2) {
			return ShadowEngagementMode.RECOVER;
		}
		if (facts.ownerHealthRatio() < .3) return ShadowEngagementMode.RESCUE;
		if (facts.preference() == ShadowRequestRange.CLOSE) return ShadowEngagementMode.CLOSE;
		if (facts.preference() == ShadowRequestRange.FAR) return ShadowEngagementMode.FAR;
		if (facts.boss() || facts.meleeDanger() > .65) return ShadowEngagementMode.FAR;
		if (facts.archetype() == ShadowTargetArchetype.FRAGILE_RANGED) return ShadowEngagementMode.CLOSE;
		return ShadowEngagementMode.SKIRMISH;
	}

	private static double base(ShadowEngagementMode mode, ShadowPowerAction action) {
		double score = 1.0 - action.cost() / 500.0;
		return score + switch (mode) {
			case CLOSE -> action.range() == ShadowPowerAction.RangeMode.CLOSE ? 5.0
					: action.intent() == ShadowPowerAction.Intent.CONTROL ? 2.0 : 0.0;
			case FAR -> action.range() == ShadowPowerAction.RangeMode.FAR ? 5.0
					: action.intent() == ShadowPowerAction.Intent.MOBILITY ? 2.0 : 0.0;
			case SKIRMISH -> action.range() == ShadowPowerAction.RangeMode.MID
					|| action.range() == ShadowPowerAction.RangeMode.FLEXIBLE ? 4.0 : 1.0;
			case RESCUE -> action.intent() == ShadowPowerAction.Intent.DEFENSE
					|| action.intent() == ShadowPowerAction.Intent.CONTROL ? 6.0 : 0.0;
			case RECOVER -> action.intent() == ShadowPowerAction.Intent.RECOVERY
					|| action.intent() == ShadowPowerAction.Intent.MOBILITY ? 6.0 : -4.0;
		};
	}

	private static Movement movement(ShadowEncounterRole role, ShadowEngagementMode mode) {
		if (role == ShadowEncounterRole.DISENGAGE) return Movement.RECOVER;
		if (role == ShadowEncounterRole.RESCUE || role == ShadowEncounterRole.PEEL) {
			return Movement.INTERPOSE;
		}
		return switch (mode) {
			case CLOSE -> Movement.APPROACH;
			case SKIRMISH -> Movement.ORBIT;
			case FAR -> Movement.RETREAT;
			case RESCUE -> Movement.INTERPOSE;
			case RECOVER -> Movement.RECOVER;
		};
	}
}

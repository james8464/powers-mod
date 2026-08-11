package com.powers.companion.combat;

import java.util.List;

/** Pure deterministic utility planner over at most the 26 already-legal actions. */
public final class ShadowTacticalPlanner {
	public enum Movement { APPROACH, ORBIT, RETREAT, INTERPOSE, RECOVER }
	public record Decision(ShadowEngagementMode mode, ShadowPowerAction action,
			Movement movement, double score, int evaluatedCount) { }

	private ShadowTacticalPlanner() {
	}

	public static Decision choose(List<ShadowPowerAction> legal, ShadowCombatFacts facts,
			ShadowLearningState learning) {
		ShadowEngagementMode mode = mode(facts);
		ShadowPowerAction best = null;
		double bestScore = -Double.MAX_VALUE;
		int evaluated = 0;
		String context = facts.contextKey(mode);
		String type = facts.archetype().name().toLowerCase();
		for (int index = 0; index < Math.min(26, legal.size()); index++) {
			ShadowPowerAction action = legal.get(index);
			evaluated++;
			if (action.cost() > facts.energyRatio() * 1_850.0) continue;
			double score = base(mode, action);
			if (facts.allyInFiringLane() && action.range() == ShadowPowerAction.RangeMode.FAR
					&& action.intent() == ShadowPowerAction.Intent.OFFENSE) score -= 10.0;
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
		return new Decision(mode, best, movement(mode), bestScore, evaluated);
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

	private static Movement movement(ShadowEngagementMode mode) {
		return switch (mode) {
			case CLOSE -> Movement.APPROACH;
			case SKIRMISH -> Movement.ORBIT;
			case FAR -> Movement.RETREAT;
			case RESCUE -> Movement.INTERPOSE;
			case RECOVER -> Movement.RECOVER;
		};
	}
}

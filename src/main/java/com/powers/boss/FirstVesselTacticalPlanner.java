package com.powers.boss;

import java.util.List;
import java.util.Map;

/** Scores a hard-capped immutable action prefix and commits no game state. */
public final class FirstVesselTacticalPlanner {
	public record Decision(FirstVesselPowerAction action, int score, int evaluatedCandidates) {
	}

	public Decision choose(List<FirstVesselPowerAction> candidates,
			FirstVesselEncounterFacts facts, int currentTick, Map<String, Integer> lastActionAt) {
		if (candidates == null || facts == null || !facts.validTarget() || currentTick < 0) return null;
		Map<String, Integer> cooldowns = lastActionAt == null ? Map.of() : lastActionAt;
		FirstVesselPowerAction best = null;
		int bestScore = Integer.MIN_VALUE;
		int evaluated = 0;
		for (FirstVesselPowerAction action : candidates) {
			if (evaluated >= FirstVesselRules.MAX_CANDIDATES) break;
			evaluated++;
			int usedAt = cooldowns.getOrDefault(action.powerId(), -1_000_000);
			if (currentTick - usedAt < action.cooldownTicks()) continue;
			int score = score(action, facts);
			if (score > bestScore) {
				best = action;
				bestScore = score;
			}
		}
		return best == null ? null : new Decision(best, bestScore, evaluated);
	}

	private static int score(FirstVesselPowerAction action, FirstVesselEncounterFacts facts) {
		int score = action.weight() * 10;
		score += switch (action.kind()) {
			case MOBILITY -> (facts.distance() > 14.0 ? 55 : -15)
					+ (facts.covered() ? 70 : 0)
					+ (facts.verticalSeparation() > 5.0 ? 35 : 0);
			case PROJECTILE -> (facts.lineOfSight() ? 50 : -100)
					+ (facts.distance() >= 8.0 && facts.distance() <= 48.0 ? 30 : -20)
					- (facts.warded() ? 100 : 0);
			case BEAM -> (facts.lineOfSight() ? 55 : -120)
					+ (facts.distance() >= 10.0 ? 35 : -25)
					+ (facts.distance() > 20.0 ? 10 : 0)
					- (facts.warded() ? 120 : 0);
			case AREA -> (facts.distance() <= 12.0 ? 60 : -85)
					+ facts.clusteredTargets() * 15 + facts.incomingProjectiles() * 4;
			case CONTROL -> (facts.targetMoving() ? 35 : 0)
					+ facts.clusteredTargets() * 8 - (facts.warded() ? 55 : 0);
			case DEFENSE -> (facts.bossHealthRatio() < 0.55 ? 90 : 0)
					+ facts.incomingProjectiles() * 8 + (facts.warded() ? 30 : 0);
			case RECOVERY -> facts.bossHealthRatio() < 0.45 ? 150 : -70;
		};
		if (action.powerId().equals(facts.previousAction())) score -= 100;
		if (action.powerId().equals("time_freeze")) {
			score += facts.clusteredTargets() * 7 + facts.incomingProjectiles() * 8;
		}
		if (action.powerId().equals("forcefield")) score += facts.incomingProjectiles() * 10;
		int jitter = Math.floorMod(java.util.Objects.hash(
				facts.variationSeed(), action.powerId()), 11);
		return score + jitter;
	}
}

package com.powers.companion;

import com.powers.companion.combat.ShadowCombatController;
import com.powers.companion.combat.ShadowPowerRuntime;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Non-sensitive bounded Shadow runtime snapshot for operators. */
public record ShadowDiagnostics(int bodies, int hiddenBodies, int revealedBodies,
		int activeTasks, int totalEnergy, int activeRites, int activeToggles,
		long casts, long conjurations, int plannerBodies, int contexts,
		int targetProfiles, int creditWindows, int forcedChunks, int leakedHandles) {
	public ShadowDiagnostics {
		bodies = nonNegative(bodies);
		hiddenBodies = nonNegative(hiddenBodies);
		revealedBodies = nonNegative(revealedBodies);
		activeTasks = nonNegative(activeTasks);
		totalEnergy = nonNegative(totalEnergy);
		activeRites = nonNegative(activeRites);
		activeToggles = nonNegative(activeToggles);
		casts = Math.max(0L, casts);
		conjurations = Math.max(0L, conjurations);
		plannerBodies = nonNegative(plannerBodies);
		contexts = nonNegative(contexts);
		targetProfiles = nonNegative(targetProfiles);
		creditWindows = nonNegative(creditWindows);
		forcedChunks = nonNegative(forcedChunks);
		leakedHandles = nonNegative(leakedHandles);
	}

	public String summary() {
		return "shadowBodies=" + bodies + " (hidden=" + hiddenBodies + ", revealed="
				+ revealedBodies + "); tasks=" + activeTasks + "; energy=" + totalEnergy
				+ "; rites=" + activeRites + "; toggles=" + activeToggles
				+ "; casts=" + casts + "; conjurations=" + conjurations
				+ "; planner=" + plannerBodies + "; contexts=" + contexts
				+ "; targetProfiles=" + targetProfiles + "; creditWindows=" + creditWindows
				+ "; forcedChunks=" + forcedChunks + "; leaks=" + leakedHandles;
	}

	static ShadowDiagnostics collect(Iterable<PrivateCompanionSession> sessions,
			Set<UUID> bodyHandles) {
		int bodies = 0;
		int revealed = 0;
		int tasks = 0;
		int energy = 0;
		Set<UUID> liveBodyIds = new HashSet<>();
		for (PrivateCompanionSession session : sessions) {
			if (session.body != null && session.body.isAlive() && !session.body.isRemoved()) {
				bodies++;
				energy += session.body.energy();
				liveBodyIds.add(session.body.getUUID());
				if (session.body.revealed()) revealed++;
			}
			if (session.tasks.active().isPresent()) tasks++;
		}
		var power = ShadowPowerRuntime.diagnostics();
		var combat = ShadowCombatController.diagnostics();
		int leaked = (int) bodyHandles.stream().filter(id -> !liveBodyIds.contains(id)).count();
		return new ShadowDiagnostics(bodies, bodies - revealed, revealed, tasks, energy,
				ShadowConjurationManager.activeCount(), power.toggles(), power.casts(),
				ShadowConjurationManager.completedCount(), combat.bodies(), combat.contexts(),
				combat.targetTypes(), combat.creditWindows(), 0, leaked);
	}

	private static int nonNegative(int value) {
		return Math.max(0, value);
	}
}

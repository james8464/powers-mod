package com.powers.companion;

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

	private static int nonNegative(int value) {
		return Math.max(0, value);
	}
}

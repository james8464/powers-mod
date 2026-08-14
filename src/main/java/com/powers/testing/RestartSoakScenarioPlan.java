package com.powers.testing;

import java.util.Set;

/** Deterministic acceptance contract shared by the restart scenario and its harness. */
public final class RestartSoakScenarioPlan {
	public enum System {
		TRAVEL_TICKET,
		BODY_PROXY,
		SPELL_FIELD,
		GUARDIAN_SUMMON,
		LIVING_FORCE_INDEX,
		TIME_FREEZE,
		CELESTIAL_RUIN,
		CONNECTED_CLIENT
	}

	public enum Shutdown { CLEAN_STOP, FLUSHED_SIGTERM }

	private static final Set<System> REQUIRED = Set.of(System.values());

	private RestartSoakScenarioPlan() {
	}

	public static Set<System> requiredSystems() {
		return REQUIRED;
	}

	public static Shutdown shutdownForCycle(int cycle) {
		if (cycle < 1) throw new IllegalArgumentException("Cycle index must be positive");
		return cycle % 12 == 0 ? Shutdown.FLUSHED_SIGTERM : Shutdown.CLEAN_STOP;
	}

	public static int rolloverLeadSeconds(int cycleSeconds) {
		if (cycleSeconds < 10) throw new IllegalArgumentException("Cycle must last at least ten seconds");
		return Math.min(30, Math.max(5, cycleSeconds / 2));
	}
}

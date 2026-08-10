package com.powers.power.artifact;

/** Pure cadence rules that stagger expensive artifact-field entity scans. */
public final class ArtifactFieldPulseRules {
	public static final int PULSE_INTERVAL = 4;
	private static final int HEAVY_INTERVAL = 20;

	private ArtifactFieldPulseRules() {
	}

	public static boolean shouldPulse(long tick, int ownerHash) {
		return Math.floorMod(tick - ownerHash, PULSE_INTERVAL) == 0;
	}

	public static boolean heavyPulse(long tick, int ownerHash) {
		return Math.floorMod(tick - ownerHash, HEAVY_INTERVAL) == 0;
	}
}

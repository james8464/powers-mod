package com.powers.mind;

import com.powers.power.travel.TravelKind;

/** Fail-closed policy for body snapshots whose dimension datapack was removed. */
public final class MissingDimensionRecoveryRules {
	private MissingDimensionRecoveryRules() { }

	public static boolean useOverworldFallback(TravelKind kind, boolean recordedDimensionAvailable) {
		return !recordedDimensionAvailable && kind == TravelKind.ADMIN_RECOVERY;
	}
}

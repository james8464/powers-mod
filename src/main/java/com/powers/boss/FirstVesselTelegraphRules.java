package com.powers.boss;

/** Accessible pre-cast windows for the First Vessel's lethal catalogue. */
public final class FirstVesselTelegraphRules {
	private FirstVesselTelegraphRules() {
	}

	public static int warningTicks(FirstVesselPowerAction action) {
		return switch (action.kind()) {
			case AREA, BEAM, PROJECTILE, CONTROL -> 30;
			case DEFENSE, MOBILITY, RECOVERY -> 20;
		};
	}
}

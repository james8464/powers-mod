package com.powers.boss;

/** One entity-safe adaptation of an innate player power. */
public record FirstVesselPowerAction(String powerId, Kind kind, int cooldownTicks,
		int weight, int minimumPhase) {
	public enum Kind {
		MOBILITY, PROJECTILE, BEAM, AREA, CONTROL, DEFENSE, RECOVERY
	}

	public FirstVesselPowerAction {
		if (powerId == null || powerId.isBlank()) throw new IllegalArgumentException("powerId");
		if (kind == null) throw new IllegalArgumentException("kind");
		cooldownTicks = Math.max(10, cooldownTicks);
		weight = Math.max(1, weight);
		minimumPhase = Math.clamp(minimumPhase, 0, 2);
	}
}

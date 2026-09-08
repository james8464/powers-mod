package com.powers.thermal;

/** Immutable entry in the total thermal transition table. */
public record ThermalTransition(ThermalInfluence influence, ThermalMaterial source,
		ThermalOutcome outcome) {
}

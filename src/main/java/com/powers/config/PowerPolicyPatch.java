package com.powers.config;

/** Nullable policy fields applied only at an explicitly configured scope. */
public record PowerPolicyPatch(
		Boolean allowTerrainDamage,
		Boolean allowBlockEntityDamage,
		Boolean hostileForcedMovement,
		Boolean requireTeleportConsent,
		Boolean requireLocatorConsent,
		Boolean requireCompanionConsent,
		Boolean requireDreamwalkConsent,
		Boolean requirePossessionConsent,
		Boolean projectionBodiesVulnerable,
		Boolean celestialRuinTerrainDamage,
		Boolean celestialRuinBlockEntityDamage) {
	public boolean isEmpty() {
		return allowTerrainDamage == null && allowBlockEntityDamage == null
				&& hostileForcedMovement == null && requireTeleportConsent == null
				&& requireLocatorConsent == null && requireCompanionConsent == null
				&& requireDreamwalkConsent == null && requirePossessionConsent == null
				&& projectionBodiesVulnerable == null && celestialRuinTerrainDamage == null
				&& celestialRuinBlockEntityDamage == null;
	}
}

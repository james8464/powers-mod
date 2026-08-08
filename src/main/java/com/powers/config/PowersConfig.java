package com.powers.config;

import java.util.List;

public record PowersConfig(
		boolean allowTerrainDamage,
		boolean allowBlockEntityDamage,
		boolean allowSelfReroll,
		boolean hostileForcedMovement,
		boolean requireTeleportConsent,
		boolean requireLocatorConsent,
		boolean requireCompanionConsent,
		boolean requireDreamwalkConsent,
		boolean requirePossessionConsent,
		boolean projectionBodiesVulnerable,
		boolean persistCooldowns,
		int wardRadius,
		int maxParticlesPerTick,
		int teleportMaxChunkDistance,
		int spaceTimeRadius,
		int chronoStopRadius,
		int rankRespecExperienceLevels,
		int adminPermissionLevel,
		List<SafeZone> safeZones) {

	public record SafeZone(String dimension, double x, double y, double z, double radius) {
		public SafeZone sanitized() {
			return new SafeZone(dimension == null ? "minecraft:overworld" : dimension,
					x, y, z, Math.max(1.0, Math.min(100_000.0, radius)));
		}
	}

	public static PowersConfig defaults() {
		return new PowersConfig(false, false, false, false,
				true, true, true, true, true, true, true,
				6, 512, 8, 32, 64, 30, 2, List.of());
	}

	public PowersConfig sanitized() {
		List<SafeZone> zones = safeZones == null ? List.of()
				: safeZones.stream().filter(java.util.Objects::nonNull).map(SafeZone::sanitized).toList();
		return new PowersConfig(allowTerrainDamage, allowBlockEntityDamage, allowSelfReroll,
				hostileForcedMovement, requireTeleportConsent, requireLocatorConsent,
				requireCompanionConsent, requireDreamwalkConsent, requirePossessionConsent,
				projectionBodiesVulnerable, persistCooldowns,
				Math.max(1, Math.min(64, wardRadius)),
				Math.max(32, Math.min(16_384, maxParticlesPerTick)),
				Math.max(1, Math.min(128, teleportMaxChunkDistance)),
				Math.max(4, Math.min(128, spaceTimeRadius)),
				Math.max(4, Math.min(256, chronoStopRadius)),
				Math.max(0, Math.min(1000, rankRespecExperienceLevels)),
				Math.max(0, Math.min(4, adminPermissionLevel)), List.copyOf(zones));
	}
}

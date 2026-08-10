package com.powers.config;

import java.util.List;

/**
 * Immutable server policy controlling terrain damage, consent-sensitive
 * effects, safe zones, summons, and administrator rerolls.
 */
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
		boolean celestialRuinTerrainDamage,
		boolean celestialRuinBlockEntityDamage,
		int wardRadius,
		int maxParticlesPerTick,
		int teleportMaxChunkDistance,
		int spaceTimeRadius,
		int chronoStopRadius,
		int rankRespecExperienceLevels,
		int adminPermissionLevel,
		List<SafeZone> safeZones,
		LivingForces livingForces,
		DialogueProvider dialogueProvider) {

	/** Sanitized pacing and safety limits for spreading realm matter. */
	public record LivingForces(boolean spreadingEnabled, int spreadAttempts, int auraRadius,
			int witherAmplifier, int energyRefillPerSecond, int clashRadius, int clashChecksPerTick) {
		public static LivingForces defaults() {
			return new LivingForces(true, 2, 8, 2, 24, 48, 4096);
		}

		public LivingForces sanitized() {
			return new LivingForces(spreadingEnabled,
					Math.max(1, Math.min(8, spreadAttempts)),
					Math.max(1, Math.min(32, auraRadius)),
					Math.max(0, Math.min(4, witherAmplifier)),
					Math.max(1, Math.min(500, energyRefillPerSecond)),
					Math.max(8, Math.min(96, clashRadius)),
					Math.max(256, Math.min(32_768, clashChecksPerTick)));
		}
	}

	public record SafeZone(String dimension, double x, double y, double z, double radius) {
		public SafeZone sanitized() {
			return new SafeZone(dimension == null ? "minecraft:overworld" : dimension,
					x, y, z, Math.max(1.0, Math.min(100_000.0, radius)));
		}
	}

	/** Disabled-by-default, bounded settings for fictional dialogue text only. */
	public record DialogueProvider(boolean enabled, String endpoint, String model,
			String credentialEnvironmentVariable, int timeoutMillis,
			int maxGlobalRequests, int ownerCooldownSeconds) {
		public static DialogueProvider defaults() {
			return new DialogueProvider(false, "", "", "POWERS_DIALOGUE_API_KEY",
					2_500, 4, 30);
		}

		public DialogueProvider sanitized() {
			String safeEndpoint = bounded(endpoint, 2_048);
			String safeModel = bounded(model, 128);
			String safeVariable = bounded(credentialEnvironmentVariable, 128);
			if (!safeVariable.matches("[A-Za-z_][A-Za-z0-9_]*")) {
				safeVariable = "POWERS_DIALOGUE_API_KEY";
			}
			return new DialogueProvider(enabled, safeEndpoint, safeModel, safeVariable,
					Math.clamp(timeoutMillis, 250, 2_500),
					Math.clamp(maxGlobalRequests, 1, 4),
					Math.clamp(ownerCooldownSeconds, 10, 3_600));
		}

		private static String bounded(String value, int maximum) {
			if (value == null) return "";
			String cleaned = value.strip();
			return cleaned.substring(0, Math.min(maximum, cleaned.length()));
		}
	}

	public static PowersConfig defaults() {
		return new PowersConfig(false, false, false, false,
				true, true, true, true, true, true, true, true, true,
				20, 512, 8, 32, 64, 30, 2, List.of(), LivingForces.defaults(),
				DialogueProvider.defaults());
	}

	public PowersConfig sanitized() {
		List<SafeZone> zones = safeZones == null ? List.of()
				: safeZones.stream().filter(java.util.Objects::nonNull).map(SafeZone::sanitized).toList();
		return new PowersConfig(allowTerrainDamage, allowBlockEntityDamage, allowSelfReroll,
				hostileForcedMovement, requireTeleportConsent, requireLocatorConsent,
				requireCompanionConsent, requireDreamwalkConsent, requirePossessionConsent,
				projectionBodiesVulnerable, persistCooldowns,
				celestialRuinTerrainDamage, celestialRuinBlockEntityDamage,
				Math.max(1, Math.min(64, wardRadius)),
				Math.max(32, Math.min(16_384, maxParticlesPerTick)),
				Math.max(1, Math.min(128, teleportMaxChunkDistance)),
				Math.max(4, Math.min(128, spaceTimeRadius)),
				Math.max(4, Math.min(256, chronoStopRadius)),
				Math.max(0, Math.min(1000, rankRespecExperienceLevels)),
				Math.max(0, Math.min(4, adminPermissionLevel)), List.copyOf(zones),
				(livingForces == null ? LivingForces.defaults() : livingForces).sanitized(),
				(dialogueProvider == null ? DialogueProvider.defaults() : dialogueProvider).sanitized());
	}
}

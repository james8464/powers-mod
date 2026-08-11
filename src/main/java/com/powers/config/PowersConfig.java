package com.powers.config;

import java.util.List;

/**
 * Immutable server policy controlling terrain damage, consent-sensitive
 * effects, safe zones, summons, and administrator rerolls.
 */
public record PowersConfig(
		int schemaVersion,
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
		boolean rankPrefixesEnabled,
		boolean celestialRuinTerrainDamage,
		boolean celestialRuinBlockEntityDamage,
		int wardRadius,
		int maxParticlesPerTick,
		int teleportMaxChunkDistance,
		int rankRespecExperienceLevels,
		int adminPermissionLevel,
		List<SafeZone> safeZones,
		TerrainScars terrainScars,
		LivingForces livingForces,
		DialogueProvider dialogueProvider) {
	public static final int CURRENT_SCHEMA_VERSION = 3;
	private static final int MAX_SAFE_ZONES = 256;

	/** Server bounds for the guaranteed environmental signature of destructive innates. */
	public record TerrainScars(int minimumTier, int maxBlocksPerCast) {
		public static TerrainScars defaults() {
			return new TerrainScars(0, 128);
		}

		public TerrainScars sanitized() {
			return new TerrainScars(Math.clamp(minimumTier, 0, 10),
					Math.clamp(maxBlocksPerCast, 1, 2_048));
		}
	}

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
			String safeDimension = dimension == null ? "" : dimension.strip();
			if (!safeDimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
				safeDimension = "minecraft:overworld";
			}
			return new SafeZone(safeDimension, finiteOrZero(x), finiteOrZero(y), finiteOrZero(z),
					Double.isFinite(radius) ? Math.clamp(radius, 1.0, 100_000.0) : 1.0);
		}

		private static double finiteOrZero(double value) {
			return Double.isFinite(value) ? value : 0.0;
		}
	}

	/** Disabled-by-default bounded endpoint for boss dialogue and low-confidence Shadow fallback. */
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
		return new PowersConfig(CURRENT_SCHEMA_VERSION, true, false, false, false,
				true, true, true, true, true, true, true, true, true, true,
				20, 512, 8, 30, 2, List.of(), TerrainScars.defaults(), LivingForces.defaults(),
				DialogueProvider.defaults());
	}

	public PowersConfig sanitized() {
		List<SafeZone> zones = safeZones == null ? List.of()
				: safeZones.stream().filter(java.util.Objects::nonNull).limit(MAX_SAFE_ZONES)
						.map(SafeZone::sanitized).toList();
		return new PowersConfig(CURRENT_SCHEMA_VERSION, allowTerrainDamage,
				allowBlockEntityDamage, allowSelfReroll,
				hostileForcedMovement, requireTeleportConsent, requireLocatorConsent,
				requireCompanionConsent, requireDreamwalkConsent, requirePossessionConsent,
				projectionBodiesVulnerable, persistCooldowns, rankPrefixesEnabled,
				celestialRuinTerrainDamage, celestialRuinBlockEntityDamage,
				Math.max(1, Math.min(64, wardRadius)),
				Math.max(32, Math.min(16_384, maxParticlesPerTick)),
				Math.max(1, Math.min(128, teleportMaxChunkDistance)),
				Math.max(0, Math.min(1000, rankRespecExperienceLevels)),
				Math.max(0, Math.min(4, adminPermissionLevel)), List.copyOf(zones),
				(terrainScars == null ? TerrainScars.defaults() : terrainScars).sanitized(),
				(livingForces == null ? LivingForces.defaults() : livingForces).sanitized(),
				(dialogueProvider == null ? DialogueProvider.defaults() : dialogueProvider).sanitized());
	}
}

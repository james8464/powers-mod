package com.powers.config;

import net.minecraft.server.level.ServerLevel;

import java.util.EnumMap;
import java.util.Map;

/** One immutable global-to-world-to-dimension gameplay-policy resolution. */
public record ResolvedPowerPolicy(
		boolean allowTerrainDamage,
		boolean allowBlockEntityDamage,
		boolean hostileForcedMovement,
		boolean requireTeleportConsent,
		boolean requireLocatorConsent,
		boolean requireCompanionConsent,
		boolean requireDreamwalkConsent,
		boolean requirePossessionConsent,
		boolean projectionBodiesVulnerable,
		boolean celestialRuinTerrainDamage,
		boolean celestialRuinBlockEntityDamage,
		Map<Field, Source> sources) {
	public enum Scope { GLOBAL, WORLD, DIMENSION }

	public enum Field {
		ALLOW_TERRAIN_DAMAGE,
		ALLOW_BLOCK_ENTITY_DAMAGE,
		HOSTILE_FORCED_MOVEMENT,
		REQUIRE_TELEPORT_CONSENT,
		REQUIRE_LOCATOR_CONSENT,
		REQUIRE_COMPANION_CONSENT,
		REQUIRE_DREAMWALK_CONSENT,
		REQUIRE_POSSESSION_CONSENT,
		PROJECTION_BODIES_VULNERABLE,
		CELESTIAL_RUIN_TERRAIN_DAMAGE,
		CELESTIAL_RUIN_BLOCK_ENTITY_DAMAGE
	}

	public record Source(Scope scope, String key) {
		public Source {
			java.util.Objects.requireNonNull(scope, "scope");
			key = key == null ? "" : key;
		}

		public String label() {
			return scope == Scope.GLOBAL ? "global"
					: scope.name().toLowerCase(java.util.Locale.ROOT) + ":" + diagnosticToken(key);
		}
	}

	public ResolvedPowerPolicy {
		sources = Map.copyOf(sources);
	}

	public static ResolvedPowerPolicy resolve(ServerLevel level) {
		return PowerPolicyResolver.resolve(level);
	}

	public static ResolvedPowerPolicy resolve(PowersConfig config, ServerLevel level) {
		String world = level.getServer().getWorldData().getLevelName();
		return resolve(config, world,
				level.dimension().identifier().toString());
	}

	public static ResolvedPowerPolicy resolve(PowersConfig config, String world, String dimension) {
		java.util.Objects.requireNonNull(config, "config");
		String safeWorld = world == null ? "" : world;
		String safeDimension = dimension == null ? "" : dimension;
		State state = new State(config);
		PowerPolicyOverrides overrides = config.policyOverrides();
		if (overrides != null) {
			PowerPolicyPatch worldPatch = overrides.worlds().get(safeWorld);
			if (worldPatch != null) {
				state.apply(worldPatch, new Source(Scope.WORLD, safeWorld));
			}
			PowerPolicyPatch dimensionPatch = overrides.dimensions().get(safeDimension);
			if (dimensionPatch != null) {
				state.apply(dimensionPatch, new Source(Scope.DIMENSION, safeDimension));
			}
		}
		return state.snapshot();
	}

	public Source source(Field field) {
		return sources.get(field);
	}

	/** Bounded operator-facing field values and the scope that supplied each one. */
	public String diagnosticLine(String world, String dimension) {
		return "resolvedPolicy world=" + diagnosticToken(world)
				+ "; dimension=" + diagnosticToken(dimension)
				+ "; terrainDamage=" + allowTerrainDamage + "@" + source(Field.ALLOW_TERRAIN_DAMAGE).label()
				+ "; blockEntityDamage=" + allowBlockEntityDamage + "@" + source(Field.ALLOW_BLOCK_ENTITY_DAMAGE).label()
				+ "; hostileMovement=" + hostileForcedMovement + "@" + source(Field.HOSTILE_FORCED_MOVEMENT).label()
				+ "; teleportConsent=" + requireTeleportConsent + "@" + source(Field.REQUIRE_TELEPORT_CONSENT).label()
				+ "; locatorConsent=" + requireLocatorConsent + "@" + source(Field.REQUIRE_LOCATOR_CONSENT).label()
				+ "; companionConsent=" + requireCompanionConsent + "@" + source(Field.REQUIRE_COMPANION_CONSENT).label()
				+ "; dreamwalkConsent=" + requireDreamwalkConsent + "@" + source(Field.REQUIRE_DREAMWALK_CONSENT).label()
				+ "; possessionConsent=" + requirePossessionConsent + "@" + source(Field.REQUIRE_POSSESSION_CONSENT).label()
				+ "; bodyVulnerable=" + projectionBodiesVulnerable + "@" + source(Field.PROJECTION_BODIES_VULNERABLE).label()
				+ "; ruinTerrain=" + celestialRuinTerrainDamage + "@" + source(Field.CELESTIAL_RUIN_TERRAIN_DAMAGE).label()
				+ "; ruinBlockEntity=" + celestialRuinBlockEntityDamage + "@" + source(Field.CELESTIAL_RUIN_BLOCK_ENTITY_DAMAGE).label();
	}

	private static String diagnosticToken(String value) {
		if (value == null || value.isBlank()) return "<none>";
		StringBuilder safe = new StringBuilder(Math.min(128, value.length()));
		for (int index = 0; index < value.length() && safe.length() < 128; index++) {
			char character = value.charAt(index);
			safe.append(character >= 0x20 && character <= 0x7e && character != ';'
					? character : '_');
		}
		return safe.toString();
	}

	private static final class State {
		private boolean terrain;
		private boolean blockEntity;
		private boolean hostileMovement;
		private boolean teleportConsent;
		private boolean locatorConsent;
		private boolean companionConsent;
		private boolean dreamwalkConsent;
		private boolean possessionConsent;
		private boolean bodyVulnerable;
		private boolean ruinTerrain;
		private boolean ruinBlockEntity;
		private final EnumMap<Field, Source> sources = new EnumMap<>(Field.class);

		private State(PowersConfig config) {
			terrain = config.allowTerrainDamage();
			blockEntity = config.allowBlockEntityDamage();
			hostileMovement = config.hostileForcedMovement();
			teleportConsent = config.requireTeleportConsent();
			locatorConsent = config.requireLocatorConsent();
			companionConsent = config.requireCompanionConsent();
			dreamwalkConsent = config.requireDreamwalkConsent();
			possessionConsent = config.requirePossessionConsent();
			bodyVulnerable = config.projectionBodiesVulnerable();
			ruinTerrain = config.celestialRuinTerrainDamage();
			ruinBlockEntity = config.celestialRuinBlockEntityDamage();
			Source global = new Source(Scope.GLOBAL, "");
			for (Field field : Field.values()) sources.put(field, global);
		}

		private void apply(PowerPolicyPatch patch, Source source) {
			if (patch.allowTerrainDamage() != null) {
				terrain = patch.allowTerrainDamage();
				sources.put(Field.ALLOW_TERRAIN_DAMAGE, source);
			}
			if (patch.allowBlockEntityDamage() != null) {
				blockEntity = patch.allowBlockEntityDamage();
				sources.put(Field.ALLOW_BLOCK_ENTITY_DAMAGE, source);
			}
			if (patch.hostileForcedMovement() != null) {
				hostileMovement = patch.hostileForcedMovement();
				sources.put(Field.HOSTILE_FORCED_MOVEMENT, source);
			}
			if (patch.requireTeleportConsent() != null) {
				teleportConsent = patch.requireTeleportConsent();
				sources.put(Field.REQUIRE_TELEPORT_CONSENT, source);
			}
			if (patch.requireLocatorConsent() != null) {
				locatorConsent = patch.requireLocatorConsent();
				sources.put(Field.REQUIRE_LOCATOR_CONSENT, source);
			}
			if (patch.requireCompanionConsent() != null) {
				companionConsent = patch.requireCompanionConsent();
				sources.put(Field.REQUIRE_COMPANION_CONSENT, source);
			}
			if (patch.requireDreamwalkConsent() != null) {
				dreamwalkConsent = patch.requireDreamwalkConsent();
				sources.put(Field.REQUIRE_DREAMWALK_CONSENT, source);
			}
			if (patch.requirePossessionConsent() != null) {
				possessionConsent = patch.requirePossessionConsent();
				sources.put(Field.REQUIRE_POSSESSION_CONSENT, source);
			}
			if (patch.projectionBodiesVulnerable() != null) {
				bodyVulnerable = patch.projectionBodiesVulnerable();
				sources.put(Field.PROJECTION_BODIES_VULNERABLE, source);
			}
			if (patch.celestialRuinTerrainDamage() != null) {
				ruinTerrain = patch.celestialRuinTerrainDamage();
				sources.put(Field.CELESTIAL_RUIN_TERRAIN_DAMAGE, source);
			}
			if (patch.celestialRuinBlockEntityDamage() != null) {
				ruinBlockEntity = patch.celestialRuinBlockEntityDamage();
				sources.put(Field.CELESTIAL_RUIN_BLOCK_ENTITY_DAMAGE, source);
			}
		}

		private ResolvedPowerPolicy snapshot() {
			return new ResolvedPowerPolicy(terrain, blockEntity, hostileMovement,
					teleportConsent, locatorConsent, companionConsent, dreamwalkConsent,
					possessionConsent, bodyVulnerable, ruinTerrain, ruinBlockEntity, sources);
		}
	}
}

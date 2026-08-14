package com.powers.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.powers.PowersMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Loads, validates, and atomically replaces the server's JSON configuration. */
public final class PowersConfigLoader {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final AtomicLong REVISION = new AtomicLong();
	private static volatile PowersConfig current = PowersConfig.defaults();
	private static volatile ConfigValidationReport validationReport = ConfigValidationReport.empty();

	public record ParseResult(PowersConfig config, ConfigValidationReport report) { }

	private PowersConfigLoader() {
	}

	public static void initialize() {
		if (!Files.exists(path())) {
			write(current);
			return;
		}
		reload();
	}

	public static PowersConfig get() {
		return current;
	}

	public static ConfigValidationReport validationReport() {
		return validationReport;
	}

	public static boolean reload() {
		try {
			ParseResult parsed = parseWithReport(Files.readString(path()));
			current = parsed.config();
			validationReport = parsed.report();
			write(current);
			PowersMod.LOGGER.info("POWERS config validation: {}", validationReport.summary());
			for (String line : validationReport.operatorLines()) PowersMod.LOGGER.warn("POWERS config {}", line);
			return true;
		} catch (Exception error) {
			PowersMod.LOGGER.error("Keeping the last valid POWERS configuration: {}", error.getMessage());
			return false;
		}
	}

	/** Installs parsed policy only for development GameTests and restores it on close. */
	static synchronized AutoCloseable installForGameTest(String json) {
		if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
			throw new IllegalStateException("GameTest configuration is unavailable in production");
		}
		PowersConfig previousConfig = current;
		ConfigValidationReport previousReport = validationReport;
		ParseResult installed = parseWithReport(json);
		current = installed.config();
		validationReport = installed.report();
		return () -> {
			synchronized (PowersConfigLoader.class) {
				if (current == installed.config()) {
					current = previousConfig;
					validationReport = previousReport;
				}
			}
		};
	}

	static PowersConfig parse(String json) {
		return parseWithReport(json).config();
	}

	static ParseResult parseWithReport(String json) {
		JsonObject object = JsonParser.parseString(json).getAsJsonObject();
		PowersConfig defaults = PowersConfig.defaults();
		List<ConfigValidationReport.Entry> changes = new ArrayList<>();
		int schemaVersion = integer(object, "schemaVersion", 0, "schemaVersion", changes);
		if (schemaVersion != PowersConfig.CURRENT_SCHEMA_VERSION) {
			changes.add(new ConfigValidationReport.Entry("schemaVersion",
					ConfigValidationReport.Kind.DEFAULTED, Integer.toString(schemaVersion),
					Integer.toString(PowersConfig.CURRENT_SCHEMA_VERSION), "schema_upgrade"));
		}
		boolean allowTerrainDamage = bool(object, "allowTerrainDamage",
				defaults.allowTerrainDamage(), "allowTerrainDamage", changes);
		if (schemaVersion < 2) {
			// Version 1 generated this setting as false even though the authored
			// combat powers promise terrain scars. Migrate that obsolete default once;
			// version-2 administrators can still opt out explicitly.
			allowTerrainDamage = true;
			changes.add(new ConfigValidationReport.Entry("allowTerrainDamage",
					ConfigValidationReport.Kind.DEFAULTED, "legacy_generated_default", "true",
					"schema_migration"));
		}
		java.util.List<PowersConfig.SafeZone> zones = defaults.safeZones();
		if (object.has("safeZones") && object.get("safeZones").isJsonArray()) {
			try {
				zones = GSON.fromJson(object.get("safeZones"),
						new TypeToken<java.util.List<PowersConfig.SafeZone>>() { }.getType());
			} catch (RuntimeException error) {
				changes.add(new ConfigValidationReport.Entry("safeZones",
						ConfigValidationReport.Kind.DEFAULTED, "<invalid:array>", "0 zones",
						"invalid_structure"));
			}
		} else if (!object.has("safeZones") || !object.get("safeZones").isJsonArray()) {
			changes.add(new ConfigValidationReport.Entry("safeZones",
					ConfigValidationReport.Kind.DEFAULTED, invalidMarker(object, "safeZones"),
					"0 zones", object.has("safeZones") ? "invalid_type" : "missing"));
		}
		PowersConfig.LivingForces forceDefaults = defaults.livingForces();
		PowersConfig.TerrainScars terrainDefaults = defaults.terrainScars();
		JsonObject terrainObject = object.has("terrainScars") && object.get("terrainScars").isJsonObject()
				? object.getAsJsonObject("terrainScars") : new JsonObject();
		PowersConfig.TerrainScars terrainScars = new PowersConfig.TerrainScars(
				integer(terrainObject, "minimumTier", terrainDefaults.minimumTier(),
						"terrainScars.minimumTier", changes),
				integer(terrainObject, "maxBlocksPerCast", terrainDefaults.maxBlocksPerCast(),
						"terrainScars.maxBlocksPerCast", changes));
		JsonObject forceObject = object.has("livingForces") && object.get("livingForces").isJsonObject()
				? object.getAsJsonObject("livingForces") : new JsonObject();
		PowersConfig.LivingForces livingForces = new PowersConfig.LivingForces(
				bool(forceObject, "spreadingEnabled", forceDefaults.spreadingEnabled(),
						"livingForces.spreadingEnabled", changes),
				integer(forceObject, "spreadAttempts", forceDefaults.spreadAttempts(),
						"livingForces.spreadAttempts", changes),
				integer(forceObject, "auraRadius", forceDefaults.auraRadius(),
						"livingForces.auraRadius", changes),
				integer(forceObject, "witherAmplifier", forceDefaults.witherAmplifier(),
						"livingForces.witherAmplifier", changes),
				integer(forceObject, "energyRefillPerSecond", forceDefaults.energyRefillPerSecond(),
						"livingForces.energyRefillPerSecond", changes),
				integer(forceObject, "clashRadius", forceDefaults.clashRadius(),
						"livingForces.clashRadius", changes),
				integer(forceObject, "clashChecksPerTick", forceDefaults.clashChecksPerTick(),
						"livingForces.clashChecksPerTick", changes));
		PowersConfig.DialogueProvider dialogueDefaults = defaults.dialogueProvider();
		JsonObject dialogueObject = object.has("dialogueProvider")
				&& object.get("dialogueProvider").isJsonObject()
				? object.getAsJsonObject("dialogueProvider") : new JsonObject();
		PowersConfig.DialogueProvider dialogueProvider = new PowersConfig.DialogueProvider(
				bool(dialogueObject, "enabled", dialogueDefaults.enabled(),
						"dialogueProvider.enabled", changes),
				string(dialogueObject, "endpoint", dialogueDefaults.endpoint(),
						"dialogueProvider.endpoint", changes),
				string(dialogueObject, "model", dialogueDefaults.model(),
						"dialogueProvider.model", changes),
				string(dialogueObject, "credentialEnvironmentVariable",
						dialogueDefaults.credentialEnvironmentVariable(),
						"dialogueProvider.credentialEnvironmentVariable", changes),
				integer(dialogueObject, "timeoutMillis", dialogueDefaults.timeoutMillis(),
						"dialogueProvider.timeoutMillis", changes),
				integer(dialogueObject, "maxGlobalRequests", dialogueDefaults.maxGlobalRequests(),
						"dialogueProvider.maxGlobalRequests", changes),
				integer(dialogueObject, "ownerCooldownSeconds", dialogueDefaults.ownerCooldownSeconds(),
						"dialogueProvider.ownerCooldownSeconds", changes));
		PowerPolicyOverrides policyOverrides = parsePolicyOverrides(object, changes);
		PowersConfig raw = new PowersConfig(
				PowersConfig.CURRENT_SCHEMA_VERSION,
				allowTerrainDamage,
				bool(object, "allowBlockEntityDamage", defaults.allowBlockEntityDamage(), "allowBlockEntityDamage", changes),
				bool(object, "allowSelfReroll", defaults.allowSelfReroll(), "allowSelfReroll", changes),
				bool(object, "hostileForcedMovement", defaults.hostileForcedMovement(), "hostileForcedMovement", changes),
				bool(object, "requireTeleportConsent", defaults.requireTeleportConsent(), "requireTeleportConsent", changes),
				bool(object, "requireLocatorConsent", defaults.requireLocatorConsent(), "requireLocatorConsent", changes),
				bool(object, "requireCompanionConsent", defaults.requireCompanionConsent(), "requireCompanionConsent", changes),
				bool(object, "requireDreamwalkConsent", defaults.requireDreamwalkConsent(), "requireDreamwalkConsent", changes),
				bool(object, "requirePossessionConsent", defaults.requirePossessionConsent(), "requirePossessionConsent", changes),
				bool(object, "projectionBodiesVulnerable", defaults.projectionBodiesVulnerable(), "projectionBodiesVulnerable", changes),
				bool(object, "persistCooldowns", defaults.persistCooldowns(), "persistCooldowns", changes),
				bool(object, "rankPrefixesEnabled", defaults.rankPrefixesEnabled(), "rankPrefixesEnabled", changes),
				bool(object, "celestialRuinTerrainDamage", defaults.celestialRuinTerrainDamage(), "celestialRuinTerrainDamage", changes),
				bool(object, "celestialRuinBlockEntityDamage", defaults.celestialRuinBlockEntityDamage(), "celestialRuinBlockEntityDamage", changes),
				integer(object, "wardRadius", defaults.wardRadius(), "wardRadius", changes),
				integer(object, "maxParticlesPerTick", defaults.maxParticlesPerTick(), "maxParticlesPerTick", changes),
				integer(object, "teleportMaxChunkDistance", defaults.teleportMaxChunkDistance(), "teleportMaxChunkDistance", changes),
				integer(object, "rankRespecExperienceLevels", defaults.rankRespecExperienceLevels(), "rankRespecExperienceLevels", changes),
				integer(object, "adminPermissionLevel", defaults.adminPermissionLevel(), "adminPermissionLevel", changes), zones,
				terrainScars, livingForces, dialogueProvider, policyOverrides);
		PowersConfig sanitized = raw.sanitized();
		recordClamps(raw, sanitized, changes);
		return new ParseResult(sanitized, ConfigValidationReport.of(REVISION.incrementAndGet(), changes));
	}

	private static PowerPolicyOverrides parsePolicyOverrides(JsonObject root,
			List<ConfigValidationReport.Entry> changes) {
		if (!root.has("policyOverrides")) {
			changes.add(new ConfigValidationReport.Entry("policyOverrides",
					ConfigValidationReport.Kind.DEFAULTED, "<missing>", "0 scoped policies", "missing"));
			return PowerPolicyOverrides.empty();
		}
		if (!root.get("policyOverrides").isJsonObject()) {
			changes.add(new ConfigValidationReport.Entry("policyOverrides",
					ConfigValidationReport.Kind.DEFAULTED, invalidMarker(root, "policyOverrides"),
					"0 scoped policies", "invalid_type"));
			return PowerPolicyOverrides.empty();
		}
		JsonObject container = root.getAsJsonObject("policyOverrides");
		return new PowerPolicyOverrides(
				parsePolicyScope(container, "worlds", false, changes),
				parsePolicyScope(container, "dimensions", true, changes));
	}

	private static Map<String, PowerPolicyPatch> parsePolicyScope(JsonObject container,
			String scope, boolean dimension, List<ConfigValidationReport.Entry> changes) {
		if (!container.has(scope)) return Map.of();
		if (!container.get(scope).isJsonObject()) {
			changes.add(new ConfigValidationReport.Entry("policyOverrides." + scope,
					ConfigValidationReport.Kind.DEFAULTED, invalidMarker(container, scope),
					"0 policies", "invalid_type"));
			return Map.of();
		}
		Map<String, PowerPolicyPatch> accepted = new LinkedHashMap<>();
		int examined = 0;
		for (Map.Entry<String, com.google.gson.JsonElement> entry
				: container.getAsJsonObject(scope).entrySet()) {
			examined++;
			if (examined > PowerPolicyOverrides.MAX_PER_SCOPE) continue;
			boolean validKey = dimension
					? PowerPolicyOverrides.validDimensionKey(entry.getKey())
					: PowerPolicyOverrides.validWorldKey(entry.getKey());
			if (!validKey || !entry.getValue().isJsonObject()) {
				changes.add(new ConfigValidationReport.Entry("policyOverrides." + scope + ".<invalid-entry>",
						ConfigValidationReport.Kind.DEFAULTED, "<redacted-entry>", "<ignored>",
						validKey ? "invalid_type" : "invalid_key"));
				continue;
			}
			PowerPolicyPatch patch = parsePolicyPatch(entry.getValue().getAsJsonObject(),
					"policyOverrides." + scope + ".<entry>", changes);
			if (!patch.isEmpty()) {
				accepted.put(dimension ? entry.getKey().strip() : entry.getKey(), patch);
			}
		}
		if (examined > PowerPolicyOverrides.MAX_PER_SCOPE) {
			changes.add(new ConfigValidationReport.Entry("policyOverrides." + scope,
					ConfigValidationReport.Kind.CLAMPED, examined + " policies",
					PowerPolicyOverrides.MAX_PER_SCOPE + " policies", "bounded_count"));
		}
		return accepted;
	}

	private static PowerPolicyPatch parsePolicyPatch(JsonObject object, String path,
			List<ConfigValidationReport.Entry> changes) {
		return new PowerPolicyPatch(
				optionalBoolean(object, "allowTerrainDamage", path, changes),
				optionalBoolean(object, "allowBlockEntityDamage", path, changes),
				optionalBoolean(object, "hostileForcedMovement", path, changes),
				optionalBoolean(object, "requireTeleportConsent", path, changes),
				optionalBoolean(object, "requireLocatorConsent", path, changes),
				optionalBoolean(object, "requireCompanionConsent", path, changes),
				optionalBoolean(object, "requireDreamwalkConsent", path, changes),
				optionalBoolean(object, "requirePossessionConsent", path, changes),
				optionalBoolean(object, "projectionBodiesVulnerable", path, changes),
				optionalBoolean(object, "celestialRuinTerrainDamage", path, changes),
				optionalBoolean(object, "celestialRuinBlockEntityDamage", path, changes));
	}

	private static Boolean optionalBoolean(JsonObject object, String key, String path,
			List<ConfigValidationReport.Entry> changes) {
		if (!object.has(key)) return null;
		try {
			if (object.get(key).isJsonPrimitive()
					&& object.getAsJsonPrimitive(key).isBoolean()) return object.get(key).getAsBoolean();
		} catch (RuntimeException ignored) {
			// The value-free validation report below owns malformed input.
		}
		changes.add(new ConfigValidationReport.Entry(path + "." + key,
				ConfigValidationReport.Kind.DEFAULTED, invalidMarker(object, key), "<unset>",
				"invalid_type"));
		return null;
	}

	private static boolean bool(JsonObject object, String key, boolean fallback, String path,
			List<ConfigValidationReport.Entry> changes) {
		try {
			if (object.has(key) && object.get(key).isJsonPrimitive()
					&& object.getAsJsonPrimitive(key).isBoolean()) return object.get(key).getAsBoolean();
		} catch (RuntimeException ignored) {
			// Report the value-free default substitution below.
		}
		changes.add(new ConfigValidationReport.Entry(path, ConfigValidationReport.Kind.DEFAULTED,
				invalidMarker(object, key), Boolean.toString(fallback),
				object.has(key) ? "invalid_type" : "missing"));
		return fallback;
	}

	private static int integer(JsonObject object, String key, int fallback, String path,
			List<ConfigValidationReport.Entry> changes) {
		try {
			if (object.has(key) && object.get(key).isJsonPrimitive()
					&& object.getAsJsonPrimitive(key).isNumber()) return object.get(key).getAsInt();
		} catch (RuntimeException ignored) {
			// Report the value-free default substitution below.
		}
		changes.add(new ConfigValidationReport.Entry(path, ConfigValidationReport.Kind.DEFAULTED,
				invalidMarker(object, key), Integer.toString(fallback),
				object.has(key) ? "invalid_type" : "missing"));
		return fallback;
	}

	private static String string(JsonObject object, String key, String fallback, String path,
			List<ConfigValidationReport.Entry> changes) {
		if (object.has(key) && object.get(key).isJsonPrimitive()
				&& object.getAsJsonPrimitive(key).isString()) return object.get(key).getAsString();
		changes.add(new ConfigValidationReport.Entry(path, ConfigValidationReport.Kind.DEFAULTED,
				invalidMarker(object, key), fallback.isEmpty() ? "<empty-default>" : "<default-string>",
				object.has(key) ? "invalid_type" : "missing"));
		return fallback;
	}

	private static void recordClamps(PowersConfig raw, PowersConfig safe,
			List<ConfigValidationReport.Entry> changes) {
		clamp(changes, "wardRadius", raw.wardRadius(), safe.wardRadius());
		clamp(changes, "maxParticlesPerTick", raw.maxParticlesPerTick(), safe.maxParticlesPerTick());
		clamp(changes, "teleportMaxChunkDistance", raw.teleportMaxChunkDistance(), safe.teleportMaxChunkDistance());
		clamp(changes, "rankRespecExperienceLevels", raw.rankRespecExperienceLevels(), safe.rankRespecExperienceLevels());
		clamp(changes, "adminPermissionLevel", raw.adminPermissionLevel(), safe.adminPermissionLevel());
		clamp(changes, "terrainScars.minimumTier", raw.terrainScars().minimumTier(), safe.terrainScars().minimumTier());
		clamp(changes, "terrainScars.maxBlocksPerCast", raw.terrainScars().maxBlocksPerCast(), safe.terrainScars().maxBlocksPerCast());
		clamp(changes, "livingForces.spreadAttempts", raw.livingForces().spreadAttempts(), safe.livingForces().spreadAttempts());
		clamp(changes, "livingForces.auraRadius", raw.livingForces().auraRadius(), safe.livingForces().auraRadius());
		clamp(changes, "livingForces.witherAmplifier", raw.livingForces().witherAmplifier(), safe.livingForces().witherAmplifier());
		clamp(changes, "livingForces.energyRefillPerSecond", raw.livingForces().energyRefillPerSecond(), safe.livingForces().energyRefillPerSecond());
		clamp(changes, "livingForces.clashRadius", raw.livingForces().clashRadius(), safe.livingForces().clashRadius());
		clamp(changes, "livingForces.clashChecksPerTick", raw.livingForces().clashChecksPerTick(), safe.livingForces().clashChecksPerTick());
		clamp(changes, "dialogueProvider.timeoutMillis", raw.dialogueProvider().timeoutMillis(), safe.dialogueProvider().timeoutMillis());
		clamp(changes, "dialogueProvider.maxGlobalRequests", raw.dialogueProvider().maxGlobalRequests(), safe.dialogueProvider().maxGlobalRequests());
		clamp(changes, "dialogueProvider.ownerCooldownSeconds", raw.dialogueProvider().ownerCooldownSeconds(), safe.dialogueProvider().ownerCooldownSeconds());
		if (!raw.dialogueProvider().credentialEnvironmentVariable().equals(
				safe.dialogueProvider().credentialEnvironmentVariable())) {
			changes.add(new ConfigValidationReport.Entry("dialogueProvider.credentialEnvironmentVariable",
					ConfigValidationReport.Kind.DEFAULTED, "<redacted-string>", "<default-string>",
					"invalid_format"));
		}
		recordStringAdjustment(changes, "dialogueProvider.endpoint",
				raw.dialogueProvider().endpoint(), safe.dialogueProvider().endpoint());
		recordStringAdjustment(changes, "dialogueProvider.model",
				raw.dialogueProvider().model(), safe.dialogueProvider().model());
		if (raw.safeZones().size() != safe.safeZones().size()) {
			changes.add(new ConfigValidationReport.Entry("safeZones", ConfigValidationReport.Kind.CLAMPED,
					raw.safeZones().size() + " zones", safe.safeZones().size() + " zones", "bounded_count"));
		}
		int compared = Math.min(raw.safeZones().size(), safe.safeZones().size());
		for (int index = 0; index < compared; index++) {
			PowersConfig.SafeZone before = raw.safeZones().get(index);
			PowersConfig.SafeZone after = safe.safeZones().get(index);
			if (before != null && !before.equals(after)) {
				changes.add(new ConfigValidationReport.Entry("safeZones[" + index + "]",
						ConfigValidationReport.Kind.CLAMPED, "<zone-definition>", "<sanitized-zone>",
						"invalid_zone_field"));
			}
		}
	}

	private static void clamp(List<ConfigValidationReport.Entry> changes,
			String path, int raw, int safe) {
		if (raw != safe) changes.add(new ConfigValidationReport.Entry(path,
				ConfigValidationReport.Kind.CLAMPED, Integer.toString(raw), Integer.toString(safe),
				"out_of_range"));
	}

	private static void recordStringAdjustment(List<ConfigValidationReport.Entry> changes,
			String path, String raw, String safe) {
		if (!raw.equals(safe)) changes.add(new ConfigValidationReport.Entry(path,
				ConfigValidationReport.Kind.CLAMPED, "<redacted-string>", "<sanitized-string>",
				"normalized_or_truncated"));
	}

	private static String invalidMarker(JsonObject object, String key) {
		if (!object.has(key) || object.get(key).isJsonNull()) return "<missing>";
		var value = object.get(key);
		if (value.isJsonArray()) return "<invalid:array>";
		if (value.isJsonObject()) return "<invalid:object>";
		if (value.isJsonPrimitive()) {
			var primitive = value.getAsJsonPrimitive();
			if (primitive.isBoolean()) return "<invalid:boolean>";
			if (primitive.isNumber()) return "<invalid:number>";
			if (primitive.isString()) return "<invalid:string>";
		}
		return "<invalid:value>";
	}

	private static void write(PowersConfig config) {
		try {
			Path path = path();
			Files.createDirectories(path.getParent());
			Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
			Files.writeString(temporary, GSON.toJson(config.sanitized()));
			Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException error) {
			PowersMod.LOGGER.error("Could not write POWERS configuration", error);
		}
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("powers.json");
	}
}

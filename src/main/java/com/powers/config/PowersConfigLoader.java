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

public final class PowersConfigLoader {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static volatile PowersConfig current = PowersConfig.defaults();

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

	public static boolean reload() {
		try {
			current = parse(Files.readString(path()));
			return true;
		} catch (Exception error) {
			PowersMod.LOGGER.error("Keeping the last valid POWERS configuration: {}", error.getMessage());
			return false;
		}
	}

	static PowersConfig parse(String json) {
		JsonObject object = JsonParser.parseString(json).getAsJsonObject();
		PowersConfig defaults = PowersConfig.defaults();
		java.util.List<PowersConfig.SafeZone> zones = defaults.safeZones();
		if (object.has("safeZones") && object.get("safeZones").isJsonArray()) {
			zones = GSON.fromJson(object.get("safeZones"),
					new TypeToken<java.util.List<PowersConfig.SafeZone>>() { }.getType());
		}
		return new PowersConfig(
				bool(object, "allowTerrainDamage", defaults.allowTerrainDamage()),
				bool(object, "allowBlockEntityDamage", defaults.allowBlockEntityDamage()),
				bool(object, "allowSelfReroll", defaults.allowSelfReroll()),
				bool(object, "hostileForcedMovement", defaults.hostileForcedMovement()),
				bool(object, "requireTeleportConsent", defaults.requireTeleportConsent()),
				bool(object, "requireLocatorConsent", defaults.requireLocatorConsent()),
				bool(object, "requireCompanionConsent", defaults.requireCompanionConsent()),
				bool(object, "projectionBodiesVulnerable", defaults.projectionBodiesVulnerable()),
				bool(object, "persistCooldowns", defaults.persistCooldowns()),
				integer(object, "wardRadius", defaults.wardRadius()),
				integer(object, "maxParticlesPerTick", defaults.maxParticlesPerTick()),
				integer(object, "teleportMaxChunkDistance", defaults.teleportMaxChunkDistance()),
				integer(object, "adminPermissionLevel", defaults.adminPermissionLevel()), zones).sanitized();
	}

	private static boolean bool(JsonObject object, String key, boolean fallback) {
		return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsBoolean() : fallback;
	}

	private static int integer(JsonObject object, String key, int fallback) {
		return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsInt() : fallback;
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

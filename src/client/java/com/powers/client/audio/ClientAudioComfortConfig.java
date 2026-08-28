package com.powers.client.audio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.powers.PowersMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/** Loads the narrow local reduced-tinnitus preference without accepting arbitrary config data. */
public final class ClientAudioComfortConfig {
	private static final int MAX_CONFIG_BYTES = 4_096;
	private static final AtomicBoolean MALFORMED_WARNING_EMITTED = new AtomicBoolean();
	private static volatile boolean reducedTinnitus;
	private static volatile Boolean acceptanceOverride;

	private ClientAudioComfortConfig() {
	}

	public static void initialize() {
		reload();
	}

	public static void reload() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("powers-client.json");
		reducedTinnitus = read(path);
	}

	public static boolean reducedTinnitus() {
		Boolean override = acceptanceOverride;
		return override == null ? reducedTinnitus : override;
	}

	/** Applies a transient development-client acceptance value without mutating user config. */
	public static void setAcceptanceOverride(boolean reduced) {
		if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
			throw new IllegalStateException("Audio comfort override is development-only");
		}
		acceptanceOverride = reduced;
	}

	/** Clears transient acceptance state at every connection or world boundary. */
	public static void clearAcceptanceOverride() {
		acceptanceOverride = null;
	}

	static boolean read(Path path) {
		try {
			if (!Files.isRegularFile(path)) return false;
			long size = Files.size(path);
			if (size < 0 || size > MAX_CONFIG_BYTES) {
				warnOnce("file exceeds 4 KiB");
				return false;
			}
			String json = Files.readString(path, StandardCharsets.UTF_8);
			JsonObject object = JsonParser.parseString(json).getAsJsonObject();
			if (!object.has("reducedTinnitus")
					|| !object.get("reducedTinnitus").isJsonPrimitive()
					|| !object.getAsJsonPrimitive("reducedTinnitus").isBoolean()) return false;
			return object.get("reducedTinnitus").getAsBoolean();
		} catch (IOException | RuntimeException error) {
			warnOnce(error.getMessage());
			return false;
		}
	}

	private static void warnOnce(String reason) {
		if (MALFORMED_WARNING_EMITTED.compareAndSet(false, true)) {
			PowersMod.LOGGER.warn("Could not load client audio comfort preference; using ordinary audio: {}",
					reason == null ? "malformed input" : reason);
		}
	}
}

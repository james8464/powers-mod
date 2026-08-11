package com.powers.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.powers.PowersMod;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Persists opt-in interaction modes; release-to-cast defaults safely off. */
public final class ClientInteractionPreferences {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir()
			.resolve("powers-interactions.json");
	private static Config current = new Config(false);

	private record Config(boolean releaseToCast) {
	}

	private ClientInteractionPreferences() {
	}

	static void initialize() {
		try {
			if (Files.exists(PATH)) {
				Config decoded = GSON.fromJson(Files.readString(PATH), Config.class);
				if (decoded != null) current = decoded;
			}
			persist();
		} catch (RuntimeException | java.io.IOException error) {
			current = new Config(false);
			PowersMod.LOGGER.warn("Could not load interaction preferences: {}", error.getMessage());
		}
	}

	public static boolean releaseToCast() {
		return current.releaseToCast();
	}

	static boolean toggleReleaseToCast() {
		current = new Config(!current.releaseToCast());
		try {
			persist();
		} catch (java.io.IOException error) {
			PowersMod.LOGGER.warn("Could not save interaction preferences: {}", error.getMessage());
		}
		return current.releaseToCast();
	}

	private static void persist() throws java.io.IOException {
		Files.createDirectories(PATH.getParent());
		Files.writeString(PATH, GSON.toJson(current));
	}
}

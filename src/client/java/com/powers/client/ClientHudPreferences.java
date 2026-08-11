package com.powers.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.powers.PowersMod;
import com.powers.hud.HudPlacement;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads the local, cosmetic HUD anchor and margin preference. */
public final class ClientHudPreferences {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static volatile HudPlacement current = HudPlacement.defaults();

	private ClientHudPreferences() {
	}

	/** Loads or creates powers-client.json; malformed input safely resets to vanilla alignment. */
	public static void initialize() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("powers-client.json");
		try {
			if (Files.exists(path)) {
				HudPlacement decoded = GSON.fromJson(Files.readString(path), HudPlacement.class);
				current = decoded == null ? HudPlacement.defaults() : new HudPlacement(decoded.anchor(),
						decoded.horizontalMargin(), decoded.verticalMargin(), decoded.powerRailMargin());
			}
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(current));
		} catch (IOException | RuntimeException error) {
			current = HudPlacement.defaults();
			PowersMod.LOGGER.warn("Could not load client HUD placement; using vanilla alignment: {}",
					error.getMessage());
		}
	}

	public static HudPlacement get() {
		return current;
	}
}

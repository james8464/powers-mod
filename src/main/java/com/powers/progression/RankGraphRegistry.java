package com.powers.progression;

import com.google.gson.Gson;
import com.powers.PowersMod;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Loads and exposes the light and darkness rank mazes from data resources. */
public final class RankGraphRegistry {
	private static final Gson GSON = new Gson();
	private static RankGraph light;
	private static RankGraph darkness;

	private RankGraphRegistry() {
	}

	public static void initialize() {
		light = load("light");
		darkness = load("darkness");
		PowersMod.LOGGER.info("Loaded rank mazes: {} light nodes, {} darkness nodes",
				light.nodes().size(), darkness.nodes().size());
	}

	public static RankGraph light() {
		return light;
	}

	public static RankGraph darkness() {
		return darkness;
	}

	private static RankGraph load(String name) {
		String path = "/data/powers/ranks/" + name + ".json";
		try (var stream = RankGraphRegistry.class.getResourceAsStream(path)) {
			if (stream == null) throw new IllegalStateException("Missing " + path);
			RankNode[] nodes = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), RankNode[].class);
			return new RankGraph(List.of(nodes));
		} catch (Exception error) {
			throw new IllegalStateException("Invalid rank graph " + name, error);
		}
	}
}

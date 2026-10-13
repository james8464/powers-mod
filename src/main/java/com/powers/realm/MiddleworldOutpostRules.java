package com.powers.realm;

import java.util.List;

/** Small, loaded-only Middleworld encounters; this is deliberately not a travel network. */
public final class MiddleworldOutpostRules {
	public static final int MAX_BLOCKS_PER_PULSE = 64;
	public static final int WEATHER_PERIOD_TICKS = 240;
	private static final List<Outpost> OUTPOSTS = List.of(
			new Outpost("between_archive", -24, 8),
			new Outpost("between_scriptorium", 8, -24),
			new Outpost("between_observatory", 40, 8));

	public enum DreamWeather { STILL, MOTES, ECHOES }

	private MiddleworldOutpostRules() {
	}

	public static List<Outpost> outposts() {
		return OUTPOSTS;
	}

	public static int maxBlocksPerPulse() {
		return MAX_BLOCKS_PER_PULSE;
	}

	/** Longest intentional path is entry → observatory; no unbounded road builder exists. */
	public static int roadLength() {
		return 32;
	}

	public static boolean createsTeleportHub() {
		return false;
	}

	public static boolean usesPrecipitation() {
		return false;
	}

	public static DreamWeather weatherAt(long gameTime) {
		return DreamWeather.values()[(int) Math.floorMod(gameTime / WEATHER_PERIOD_TICKS,
				DreamWeather.values().length)];
	}

	public record Outpost(String id, int x, int z) {
		public Outpost {
			if (id == null || id.isBlank() || Math.abs(x) > 64 || Math.abs(z) > 64) {
				throw new IllegalArgumentException("Middleworld outposts must remain in the bounded arrival area");
			}
		}
	}
}

package com.powers.item.artifact;

import java.util.Map;
import java.util.Set;

/** Pure compatibility policy for one persisted artifact selection key. */
public final class ArtifactSelectionMigration {
	private static final String FALLBACK = "innate/lightning_strike";
	private static final String CALL = "unique/call_hollowed";
	private static final String BLIGHT = "unique/blight_ground";
	private static final String NIGHTFALL = "unique/nightfall_dominion";
	private static final Map<String, String> UTILITY_ALIASES = Map.ofEntries(
			Map.entry("legacy/summon_darkness", CALL),
			Map.entry("unique/summon_darkness", CALL),
			Map.entry("dominion/call_hollowed", CALL),
			Map.entry("summon_darkness", CALL),
			Map.entry("call_hollowed", CALL),
			Map.entry("legacy/spread_darkness", BLIGHT),
			Map.entry("unique/spread_darkness", BLIGHT),
			Map.entry("dominion/blight_ground", BLIGHT),
			Map.entry("spread_darkness", BLIGHT),
			Map.entry("blight_ground", BLIGHT));
	private static final Set<String> DESTRUCTIVE_ALIASES = Set.of(
			"legacy/abyssal_singularity", "legacy/oblivion_pulse",
			"legacy/annihilation_beam", "legacy/soul_requiem",
			"legacy/nightfall_dominion", "abyssal_singularity", "oblivion_pulse",
			"annihilation_beam", "soul_requiem", "nightfall_dominion",
			"dominion/night_chain", "dominion/eclipse_wave", "dominion/devour_light",
			"dominion/black_decree", "dominion/event_horizon", "dominion/legion_eclipse");
	private static final Set<String> RETIRED_SAFE_ALIASES = Set.of(
			"dominion/umbral_step", "dominion/abyss_gate", "dominion/deathless_night");

	private ArtifactSelectionMigration() {
	}

	/**
	 * Retains current catalogue keys and deterministically folds retired Darkness
	 * originals into the three canonical choices without bypassing rank ten.
	 */
	public static String migrate(ArtifactAlignment alignment, String storedKey, int rank) {
		if (alignment == ArtifactAlignment.DARKNESS && NIGHTFALL.equals(storedKey)) {
			return rank >= 10 ? NIGHTFALL : CALL;
		}
		if (ArtifactActionCatalogue.find(alignment, storedKey) != null) return storedKey;
		if (alignment != ArtifactAlignment.DARKNESS || storedKey == null) return FALLBACK;
		String utility = UTILITY_ALIASES.get(storedKey);
		if (utility != null) return utility;
		if (DESTRUCTIVE_ALIASES.contains(storedKey)) return rank >= 10 ? NIGHTFALL : CALL;
		if (RETIRED_SAFE_ALIASES.contains(storedKey)) return CALL;
		return FALLBACK;
	}
}

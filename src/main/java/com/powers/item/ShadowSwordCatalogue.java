package com.powers.item;

import com.powers.power.crystals.CrystalAbilityCatalog;

import java.util.ArrayList;
import java.util.List;

/** Stable menu plan for every innate/crystal action exposed by the Shadow Sword. */
public final class ShadowSwordCatalogue {
	public enum Source { INNATE, CRYSTAL, COMMAND, DARKNESS }

	public record Definition(String key, String abilityId, Source source, int requiredDarknessRank) {
	}

	private static final List<String> INNATE_IDS = List.of(
			"size_shift", "time_shift", "shadow_step", "flight", "elemental_blast",
			"starfall", "void_beam", "fireball", "frost_nova", "lightning_strike",
			"ground_slam", "thunderclap", "speed_burst", "telekinesis", "energy_beam", "super_speed",
			"breezy_bash", "cozy_campfire", "invisibility", "time_freeze", "forcefield",
			"gravity_displacement", "vessel_possession", "astral_projection", "energy_drain",
			"ice_manipulation", "plant_healing_acceleration", "double_health");

	private ShadowSwordCatalogue() {
	}

	/** Returns menu entries in deterministic lore-friendly order. */
	public static List<Definition> definitions() {
		List<Definition> result = new ArrayList<>();
		INNATE_IDS.forEach(id -> result.add(new Definition("innate/" + id, id, Source.INNATE, 1)));
		CrystalAbilityCatalog.defaults().values().stream().flatMap(List::stream).distinct()
				.forEach(id -> result.add(new Definition("crystal/" + id, id, Source.CRYSTAL, 1)));
		result.add(new Definition("command/summon_darkness", "summon_darkness", Source.COMMAND, 1));
		result.add(new Definition("command/spread_darkness", "spread_darkness", Source.COMMAND, 1));
		result.add(new Definition("darkness/abyssal_singularity", "abyssal_singularity", Source.DARKNESS, 3));
		result.add(new Definition("darkness/oblivion_pulse", "oblivion_pulse", Source.DARKNESS, 5));
		result.add(new Definition("darkness/annihilation_beam", "annihilation_beam", Source.DARKNESS, 7));
		result.add(new Definition("darkness/soul_requiem", "soul_requiem", Source.DARKNESS, 9));
		result.add(new Definition("darkness/nightfall_dominion", "nightfall_dominion", Source.DARKNESS, 10));
		return List.copyOf(result);
	}
}

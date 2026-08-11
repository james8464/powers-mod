package com.powers.item.artifact;

import com.powers.magic.MagicSignificance;
import com.powers.power.PowerEnergy;
import com.powers.power.crystals.CrystalAbilityCatalog;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Complete deterministic action roster for both opposed mythic artifacts. */
public final class ArtifactActionCatalogue {
	private static final List<String> INNATE_IDS = List.of(
			"size_shift", "time_shift", "flight", "starfall", "void_beam", "fireball",
			"lightning_strike", "thunderclap", "speed_burst", "telekinesis", "energy_beam",
			"super_speed", "breezy_bash", "invisibility", "time_freeze",
			"forcefield", "gravity_displacement", "vessel_possession", "astral_projection",
			"energy_drain", "ice_manipulation", "plant_healing_acceleration", "double_health");
	private static final List<String> LIGHT_INNATE_IDS = List.of(
			"flight", "starfall", "lightning_strike", "thunderclap", "energy_beam",
			"forcefield", "plant_healing_acceleration", "double_health");
	private static final List<String> LIGHT_CRYSTAL_IDS = List.of(
			"creativity_manifestation", "life_bloom", "light_crystal");
	private static final Map<ArtifactAlignment, List<ArtifactActionDefinition>> BY_ALIGNMENT = build();

	private ArtifactActionCatalogue() {
	}

	public static List<ArtifactActionDefinition> forAlignment(ArtifactAlignment alignment) {
		return BY_ALIGNMENT.getOrDefault(alignment, List.of());
	}

	public static List<ArtifactActionDefinition> all() {
		return BY_ALIGNMENT.values().stream().flatMap(List::stream).toList();
	}

	public static ArtifactActionDefinition find(ArtifactAlignment alignment, String key) {
		if (key == null) return null;
		return forAlignment(alignment).stream().filter(action -> action.key().equals(key))
				.findFirst().orElse(null);
	}

	private static Map<ArtifactAlignment, List<ArtifactActionDefinition>> build() {
		Map<ArtifactAlignment, List<ArtifactActionDefinition>> result =
				new EnumMap<>(ArtifactAlignment.class);
		for (ArtifactAlignment alignment : ArtifactAlignment.values()) {
			List<ArtifactActionDefinition> actions = new ArrayList<>();
			List<String> innateIds = alignment == ArtifactAlignment.DARKNESS
					? INNATE_IDS : LIGHT_INNATE_IDS;
			for (String id : innateIds) actions.add(routed(alignment, id,
					ArtifactActionCategory.ROUTED_POWER));
			java.util.stream.Stream<String> crystalIds = alignment == ArtifactAlignment.DARKNESS
					? CrystalAbilityCatalog.defaults().values().stream().flatMap(List::stream).distinct()
					: LIGHT_CRYSTAL_IDS.stream();
			crystalIds.forEach(id -> actions.add(routed(alignment, id,
					ArtifactActionCategory.ROUTED_CRYSTAL)));
			if (alignment == ArtifactAlignment.DARKNESS) addDarkness(actions);
			else addLight(actions);
			validateUnique(actions);
			result.put(alignment, List.copyOf(actions));
		}
		return Map.copyOf(result);
	}

	private static ArtifactActionDefinition routed(ArtifactAlignment alignment, String id,
			ArtifactActionCategory category) {
		String prefix = category == ArtifactActionCategory.ROUTED_POWER ? "innate/" : "crystal/";
		return new ArtifactActionDefinition(prefix + id, id, category, alignment, 1,
				PowerEnergy.baseCost(id), 0, MagicSignificance.MINIMAL);
	}

	private static void addDarkness(List<ArtifactActionDefinition> actions) {
		add(actions, ArtifactAlignment.DARKNESS, "unique/call_hollowed", "call_hollowed",
				1, 18, 400, MagicSignificance.STANDARD);
		add(actions, ArtifactAlignment.DARKNESS, "unique/blight_ground", "blight_ground",
				2, 20, 240, MagicSignificance.STANDARD);
		add(actions, ArtifactAlignment.DARKNESS, "unique/nightfall_dominion", "nightfall_dominion",
				10, 100, 0, MagicSignificance.COSMIC);
	}

	private static void addLight(List<ArtifactActionDefinition> actions) {
		addDominionSet(actions, ArtifactAlignment.LIGHT, List.of(
				new Dominion("call_radiant", 1, 18, 400, MagicSignificance.STANDARD),
				new Dominion("consecrate_ground", 2, 20, 240, MagicSignificance.STANDARD),
				new Dominion("covenant_chain", 4, 25, 360, MagicSignificance.STANDARD),
				new Dominion("daybreak_wave", 5, 32, 500, MagicSignificance.RITUAL),
				new Dominion("heaven_gate", 6, 40, 900, MagicSignificance.RITUAL),
				new Dominion("solar_firmament", 8, 60, 1200, MagicSignificance.COSMIC),
				new Dominion("second_dawn", 9, 80, 2400, MagicSignificance.RITUAL),
				new Dominion("host_heaven", 10, 100, 3600, MagicSignificance.COSMIC)));
	}

	private static void addDominionSet(List<ArtifactActionDefinition> actions,
			ArtifactAlignment alignment, List<Dominion> definitions) {
		for (Dominion definition : definitions) {
			add(actions, alignment, "dominion/" + definition.id, definition.id,
					definition.rank, definition.cost, definition.cooldown, definition.significance);
		}
	}

	private static void add(List<ArtifactActionDefinition> actions, ArtifactAlignment alignment,
			String key, String id, int rank, int cost, int cooldown, MagicSignificance significance) {
		actions.add(new ArtifactActionDefinition(key, id, ArtifactActionCategory.DOMINION,
				alignment, rank, cost, cooldown, significance));
	}

	private static void validateUnique(List<ArtifactActionDefinition> actions) {
		Map<String, ArtifactActionDefinition> keys = new LinkedHashMap<>();
		for (ArtifactActionDefinition action : actions) {
			if (keys.putIfAbsent(action.key(), action) != null) {
				throw new IllegalStateException("Duplicate artifact action: " + action.key());
			}
		}
	}

	private record Dominion(String id, int rank, int cost, int cooldown,
			MagicSignificance significance) {
	}
}

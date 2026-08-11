package com.powers.companion.combat;

import com.powers.power.Power;
import com.powers.power.PowerEnergy;
import com.powers.power.PowerRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Exact non-crystal Shadow arsenal: every innate plus the three sword uniques. */
public final class ShadowPowerCatalogue {
	private static final List<String> UNIQUES = List.of(
			"call_hollowed", "blight_ground", "nightfall_dominion");

	private ShadowPowerCatalogue() {
	}

	public static List<ShadowPowerAction> actions() {
		List<ShadowPowerAction> result = new ArrayList<>(26);
		for (Power power : PowerRegistry.getAll()) result.add(metadata(power.id().getPath()));
		for (String unique : UNIQUES) result.add(metadata(unique));
		return List.copyOf(result);
	}

	public static ShadowPowerAction find(String id) {
		return actions().stream().filter(action -> action.id().equals(id)).findFirst().orElse(null);
	}

	public static void requireComplete() {
		List<ShadowPowerAction> actions = actions();
		if (actions.size() != 26) throw new IllegalStateException("Shadow arsenal must contain 26 actions");
		Set<String> unique = new HashSet<>();
		for (ShadowPowerAction action : actions) {
			if (!unique.add(action.id()) || ShadowPowerExecutor.handler(action.id())
					== ShadowPowerExecutor.Handler.UNSUPPORTED) {
				throw new IllegalStateException("Incomplete Shadow action: " + action.id());
			}
		}
	}

	private static ShadowPowerAction metadata(String id) {
		var range = switch (id) {
			case "thunderclap", "breezy_bash", "speed_burst", "super_speed" -> ShadowPowerAction.RangeMode.CLOSE;
			case "fireball", "lightning_strike", "starfall", "void_beam", "energy_beam",
					"ice_manipulation" -> ShadowPowerAction.RangeMode.FAR;
			case "telekinesis", "gravity_displacement", "energy_drain", "vessel_possession" -> ShadowPowerAction.RangeMode.MID;
			case "time_shift", "flight", "astral_projection" -> ShadowPowerAction.RangeMode.FLEXIBLE;
			default -> ShadowPowerAction.RangeMode.SELF;
		};
		var intent = switch (id) {
			case "time_shift", "flight", "speed_burst", "super_speed", "astral_projection" -> ShadowPowerAction.Intent.MOBILITY;
			case "forcefield", "double_health", "size_shift" -> ShadowPowerAction.Intent.DEFENSE;
			case "plant_healing_acceleration", "energy_drain" -> ShadowPowerAction.Intent.RECOVERY;
			case "telekinesis", "gravity_displacement", "vessel_possession", "time_freeze",
					"invisibility" -> ShadowPowerAction.Intent.CONTROL;
			case "call_hollowed" -> ShadowPowerAction.Intent.SUMMON;
			case "blight_ground" -> ShadowPowerAction.Intent.TERRAIN;
			default -> ShadowPowerAction.Intent.OFFENSE;
		};
		boolean toggle = Set.of("size_shift", "flight", "super_speed", "invisibility",
				"time_freeze", "forcefield", "double_health", "nightfall_dominion").contains(id);
		var work = switch (id) {
			case "time_freeze" -> ShadowPowerAction.WorkClass.GLOBAL;
			case "blight_ground", "thunderclap", "starfall", "fireball", "void_beam",
					"energy_beam" -> ShadowPowerAction.WorkClass.TERRAIN;
			case "call_hollowed" -> ShadowPowerAction.WorkClass.ENTITY_QUERY;
			case "lightning_strike", "ice_manipulation" -> ShadowPowerAction.WorkClass.PROJECTILE;
			default -> ShadowPowerAction.WorkClass.CHEAP;
		};
		int destruction = intent == ShadowPowerAction.Intent.OFFENSE
				|| intent == ShadowPowerAction.Intent.TERRAIN ? 10 : 0;
		return new ShadowPowerAction(id, range, intent, destruction, toggle, work,
				PowerEnergy.baseCost(id));
	}
}

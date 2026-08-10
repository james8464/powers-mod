package com.powers.boss;

import java.util.List;

import static com.powers.boss.FirstVesselPowerAction.Kind.AREA;
import static com.powers.boss.FirstVesselPowerAction.Kind.BEAM;
import static com.powers.boss.FirstVesselPowerAction.Kind.CONTROL;
import static com.powers.boss.FirstVesselPowerAction.Kind.DEFENSE;
import static com.powers.boss.FirstVesselPowerAction.Kind.MOBILITY;
import static com.powers.boss.FirstVesselPowerAction.Kind.PROJECTILE;
import static com.powers.boss.FirstVesselPowerAction.Kind.RECOVERY;

/** Complete adapter manifest for all 28 innate powers. */
public final class FirstVesselPowerCatalogue {
	private static final List<FirstVesselPowerAction> ACTIONS = List.of(
			a("size_shift", DEFENSE, 180, 3, 0),
			a("time_shift", MOBILITY, 100, 5, 0),
			a("shadow_step", MOBILITY, 80, 6, 0),
			a("flight", MOBILITY, 120, 3, 0),
			a("elemental_blast", PROJECTILE, 60, 7, 0),
			a("starfall", AREA, 180, 4, 1),
			a("void_beam", BEAM, 100, 6, 1),
			a("fireball", PROJECTILE, 50, 7, 0),
			a("frost_nova", AREA, 120, 6, 0),
			a("lightning_strike", PROJECTILE, 40, 8, 0),
			a("ground_slam", AREA, 100, 7, 0),
			a("thunderclap", AREA, 90, 7, 0),
			a("speed_burst", MOBILITY, 70, 5, 0),
			a("telekinesis", CONTROL, 80, 7, 0),
			a("energy_beam", BEAM, 80, 7, 0),
			a("super_speed", MOBILITY, 80, 5, 0),
			a("breezy_bash", CONTROL, 80, 6, 0),
			a("cozy_campfire", RECOVERY, 240, 2, 0),
			a("invisibility", DEFENSE, 160, 3, 1),
			a("time_freeze", CONTROL, 240, 2, 2),
			a("forcefield", DEFENSE, 180, 4, 0),
			a("gravity_displacement", CONTROL, 120, 6, 1),
			a("vessel_possession", CONTROL, 180, 3, 1),
			a("astral_projection", MOBILITY, 140, 3, 1),
			a("energy_drain", BEAM, 100, 6, 1),
			a("ice_manipulation", PROJECTILE, 80, 6, 0),
			a("plant_healing_acceleration", RECOVERY, 220, 3, 0),
			a("double_health", DEFENSE, 300, 2, 2));

	private FirstVesselPowerCatalogue() {
	}

	public static List<FirstVesselPowerAction> actions() {
		return ACTIONS;
	}

	/** Phase decks are deliberately capped even though the complete manifest is larger. */
	public static List<FirstVesselPowerAction> deck(FirstVesselPhase phase) {
		int phaseIndex = phase.ordinal();
		List<FirstVesselPowerAction> eligible = ACTIONS.stream()
				.filter(action -> action.minimumPhase() <= phaseIndex).toList();
		if (eligible.size() <= FirstVesselRules.MAX_CANDIDATES) return eligible;
		int offset = phaseIndex == 2 ? eligible.size() - FirstVesselRules.MAX_CANDIDATES : 0;
		return eligible.subList(offset, offset + FirstVesselRules.MAX_CANDIDATES);
	}

	private static FirstVesselPowerAction a(String id, FirstVesselPowerAction.Kind kind,
			int cooldown, int weight, int phase) {
		return new FirstVesselPowerAction(id, kind, cooldown, weight, phase);
	}
}

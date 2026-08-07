package com.powers.power;

import net.minecraft.resources.Identifier;

/** Balances the shared energy reserve without coupling abilities to UI code. */
public final class PowerEnergy {
	public static final int BASE_MAX = 250;
	public static final int DARKNESS_BASE_MAX = BASE_MAX * 2;

	private PowerEnergy() {
	}

	/** +52 per rank: 250 base, 770 at the rank-10 cap (was 775 at 21 ranks). */
	public static int maxCapacity(int level) {
		return BASE_MAX + Math.max(0, level) * 52;
	}

	/** +135 per rank: 500 base, 1850 at the rank-10 cap (was 1850 at 30 ranks). */
	public static int darknessMaxCapacity(int level) {
		return DARKNESS_BASE_MAX + Math.max(0, level) * 135;
	}

	public static int darknessRegen(boolean inDarkEnvironment) {
		return inDarkEnvironment ? 4 : 2;
	}

	public static int cost(Ability ability) {
		Identifier id = ability.id();
		return switch (id.getPath()) {
			case "lightning_strike", "fireball" -> 4;
			case "speed_burst", "shadow_step", "super_speed", "invisibility" -> 10;
			case "energy_beam", "void_beam", "frost_nova", "ice_manipulation" -> 22;
			case "elemental_blast", "gravity_displacement", "breezy_bash" -> 28;
			case "ground_slam", "forcefield", "cozy_campfire" -> 35;
			case "starfall", "slow_world", "time_freeze", "dimensional_anchor" -> 45;
			case "telekinesis", "vessel_possession" -> 24;
			case "astral_projection" -> 32;
			case "plant_healing_acceleration", "double_health" -> 24;
			case "energy_drain" -> 30;
			case "time_shift" -> 18;
			default -> 20;
		};
	}

	public static int ongoingCost(Ability ability) {
		return switch (ability.id().getPath()) {
			case "flight", "invisibility" -> 1;
			case "time_freeze" -> 3;
			default -> 0;
		};
	}
}

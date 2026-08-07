package com.powers.power;

import net.minecraft.resources.Identifier;

/** the shared energy pool every power draws from, plus the costs */
public final class PowerEnergy {
	// starting energy pool for every player
	public static final int BASE_MAX = 250;
	// the shadow variant starts with twice the base pool
	public static final int DARKNESS_BASE_MAX = BASE_MAX * 2;

	private PowerEnergy() {
	}

	/** pool grows 52 per rank: 250 base, 770 at the rank-10 cap */
	public static int maxCapacity(int level) {
		return BASE_MAX + Math.max(0, level) * 52;
	}

	/** shadow pool grows 135 per rank: 500 base, 1850 at the rank-10 cap */
	public static int darknessMaxCapacity(int level) {
		return DARKNESS_BASE_MAX + Math.max(0, level) * 135;
	}

	/** darkness recharges faster in the dark: 4 per tick there, 2 in daylight */
	public static int darknessRegen(boolean inDarkEnvironment) {
		return inDarkEnvironment ? 4 : 2;
	}

	/** one-time energy cost to fire the ability, with a default of 20 for anything unlisted */
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

	/** per-tick drain while a toggle like flight stays on, zero for everything else */
	public static int ongoingCost(Ability ability) {
		return switch (ability.id().getPath()) {
			case "flight", "invisibility" -> 1;
			case "time_freeze" -> 3;
			default -> 0;
		};
	}
}

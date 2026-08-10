package com.powers.power;

import com.powers.player.PlayerPowers;
import com.powers.power.abilities.SizeMorphAbility;
import com.powers.power.abilities.SizeMorphRules;
import com.powers.power.abilities.TimeFreezeDrainRules;
import com.powers.progression.PowerScalingService;
import com.powers.power.artifact.AlignedArtifactAbility;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.magic.runtime.CastScalingContext;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

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
		return growingCapacity(BASE_MAX, level, 52);
	}

	/** shadow pool grows 135 per rank: 500 base, 1850 at the rank-10 cap */
	public static int darknessMaxCapacity(int level) {
		return growingCapacity(DARKNESS_BASE_MAX, level, 135);
	}

	private static int growingCapacity(int base, int level, int perLevel) {
		long result = base + Math.max(0L, (long) level) * perLevel;
		return (int) Math.min(Integer.MAX_VALUE, result);
	}

	/** darkness recharges faster in the dark: 4 per tick there, 2 in daylight */
	public static int darknessRegen(boolean inDarkEnvironment) {
		return inDarkEnvironment ? 4 : 2;
	}

	/** one-time energy cost to fire the ability, with a default of 20 for anything unlisted */
	public static int cost(Ability ability) {
		return baseCost(ability.id().getPath());
	}

	/** Returns the unscaled authored cost for menus and server-side artifact routing. */
	public static int baseCost(String abilityId) {
		return switch (abilityId) {
			case "lightning_strike", "fireball" -> 4;
			case "speed_burst", "super_speed", "invisibility" -> 10;
			case "energy_beam", "void_beam", "ice_manipulation" -> 22;
			case "gravity_displacement", "breezy_bash", "thunderclap" -> 28;
			case "forcefield" -> 35;
			case "starfall", "time_freeze", "dimensional_anchor" -> 45;
			case "telekinesis", "vessel_possession" -> 24;
			case "astral_projection" -> 32;
			case "plant_healing_acceleration", "double_health" -> 24;
			case "energy_drain" -> 30;
			case "time_shift" -> 18;
			case "call_hollowed", "call_radiant" -> 18;
			case "blight_ground", "consecrate_ground" -> 20;
			case "umbral_step", "dawnstride" -> 12;
			case "night_chain", "covenant_chain" -> 25;
			case "eclipse_wave", "daybreak_wave" -> 32;
			case "abyss_gate", "heaven_gate" -> 40;
			case "devour_light", "banish_darkness" -> 35;
			case "black_decree", "divine_decree" -> 55;
			case "event_horizon", "solar_firmament" -> 60;
			case "deathless_night", "second_dawn" -> 80;
			case "legion_eclipse", "host_heaven" -> 100;
			case "nightfall_dominion" -> 100;
			default -> 20;
		};
	}

	/** Applies rank efficiency only to innate player powers, never crystals. */
	public static int cost(ServerPlayer player, Ability ability) {
		if (ability instanceof AlignedArtifactAbility artifact
				&& artifact.definition().alignment() == ArtifactAlignment.DARKNESS
				&& PlayerPowers.get(player).darknessLevel() >= 10) {
			return Math.max(1, (int) Math.ceil(cost(ability) * 0.8));
		}
		return CastScalingContext.currentSource().appliesPlayerRank(ability.usesRankScaling())
				? PowerScalingService.energyCost(player, ability.id().getPath(), cost(ability))
				: cost(ability);
	}

	/** per-tick drain while a toggle like flight stays on, zero for everything else */
	public static int ongoingCost(Ability ability) {
		return switch (ability.id().getPath()) {
			case "size_shift", "flight", "invisibility" -> 1;
			case "forcefield", "double_health" -> 2;
			case "time_freeze" -> 3;
			case "nightfall_dominion" -> 12;
			default -> 0;
		};
	}

	/** Applies rank efficiency to an innate toggle's recurring server-side drain. */
	public static int ongoingCost(ServerPlayer player, Ability ability) {
		if (ability.id().getPath().equals("time_freeze")) {
			// Freezing every dimension is intentionally exceptional: rank efficiency and
			// artifact routing cannot turn global clock ownership into a cheap toggle.
			return TimeFreezeDrainRules.energyPerSecond(PlayerPowers.get(player).energyCapacity());
		}
		int baseCost = ability instanceof SizeMorphAbility
				? SizeMorphRules.energyDrainPerSecond(SizeMorphRules.scale(
						PlayerPowers.get(player).getSizeMorphOption()))
				: ongoingCost(ability);
		return CastScalingContext.currentSource().appliesPlayerRank(ability.usesRankScaling())
				? PowerScalingService.energyCost(player, ability.id().getPath(), baseCost)
				: baseCost;
	}
}

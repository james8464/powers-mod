package com.powers.power;

import com.powers.PowersMod;
import com.powers.power.abilities.AstralProjectionAbility;
import com.powers.power.abilities.BreezyBashAbility;
import com.powers.power.abilities.DoubleHealthAbility;
import com.powers.power.abilities.EnergyBeamAbility;
import com.powers.power.abilities.EnergyDrainAbility;
import com.powers.power.abilities.FireballAbility;
import com.powers.power.abilities.FlightAbility;
import com.powers.power.abilities.ForcefieldAbility;
import com.powers.power.abilities.GravityDisplacementAbility;
import com.powers.power.abilities.IceManipulationAbility;
import com.powers.power.abilities.InvisibilityToggleAbility;
import com.powers.power.abilities.LightningStrikeAbility;
import com.powers.power.abilities.PlantHealingAbility;
import com.powers.power.abilities.SizeMorphAbility;
import com.powers.power.abilities.SpeedBurstAbility;
import com.powers.power.abilities.StarfallAbility;
import com.powers.power.abilities.SuperSpeedAbility;
import com.powers.power.abilities.TelekinesisAbility;
import com.powers.power.abilities.TeleportAbility;
import com.powers.power.abilities.ThunderclapAbility;
import com.powers.power.abilities.TimeFreezeToggleAbility;
import com.powers.power.abilities.VesselPossessionAbility;
import com.powers.power.abilities.VoidBeamAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Every randomly assigned innate power. Automatic passive effects are
 * deliberately excluded: a power changes gameplay only while explicitly used.
 */
public final class PowerRegistry {
	// LinkedHashMap keeps registration order so the power list is stable
	private static final Map<Identifier, Power> POWERS = new LinkedHashMap<>();

	private PowerRegistry() {
	}

	public static void initialize() {
		POWERS.clear();
		register("size_shift", 0xFFD600, new SizeMorphAbility());
		register("time_shift", 0xFFD700, new TeleportAbility());
		register("flight", 0xFFFFFF, new FlightAbility());
		register("starfall", 0x3949AB, new StarfallAbility(), PowerAffinity.RADIANT);
		register("void_beam", 0x1A237E, new VoidBeamAbility(), PowerAffinity.DARKNESS);
		register("fireball", 0xFF4500, new FireballAbility());
		register("lightning_strike", 0x4FC3F7, new LightningStrikeAbility());
		register("thunderclap", 0xD7F8FF, new ThunderclapAbility());
		register("speed_burst", 0xFFEB3B, new SpeedBurstAbility());
		register("telekinesis", 0x9C27B0, new TelekinesisAbility());
		register("energy_beam", 0xFF4500, new EnergyBeamAbility());
		register("super_speed", 0x00E5FF, new SuperSpeedAbility());
		register("breezy_bash", 0xB0BEC5, new BreezyBashAbility());
		register("invisibility", 0xAAAAAA, new InvisibilityToggleAbility());
		register("time_freeze", 0x80DEEA, new TimeFreezeToggleAbility());
		register("forcefield", 0x40C4FF, new ForcefieldAbility());
		register("gravity_displacement", 0x7C4DFF, new GravityDisplacementAbility());
		register("vessel_possession", 0xE040FB, new VesselPossessionAbility());
		register("astral_projection", 0x7E57C2, new AstralProjectionAbility());
		register("energy_drain", 0x6A1B9A, new EnergyDrainAbility(), PowerAffinity.DARKNESS);
		register("ice_manipulation", 0x81D4FA, new IceManipulationAbility());
		register("plant_healing_acceleration", 0x66FF66, new PlantHealingAbility(), PowerAffinity.RADIANT);
		register("double_health", 0xFF1744, new DoubleHealthAbility());
	}

	private static void register(String path, int color, Ability ability) {
		register(path, color, ability, PowerAffinity.UNIVERSAL);
	}

	private static void register(String path, int color, Ability ability, PowerAffinity affinity) {
		register(new Power(PowersMod.id(path), Component.translatable("power.powers." + path),
				Component.translatable("power.powers." + path + ".description"), color,
				ability, affinity));
	}

	private static void register(Power power) {
		POWERS.put(power.id(), power);
	}

	public static List<Power> getAll() {
		return new ArrayList<>(POWERS.values());
	}

	/** the powers a player can draw from when the game picks their three */
	public static List<Power> getAssignable() {
		return getAll();
	}

	/** Returns only powers that may occupy an innate slot for this allegiance. */
	public static List<Power> getAssignable(PowerAffinity allegiance) {
		return POWERS.values().stream()
				.filter(power -> power.affinity().permits(allegiance))
				.toList();
	}

	public static Power get(Identifier id) {
		return POWERS.get(id);
	}

	public static Power get(String idString) {
		if (idString == null || idString.isBlank()) {
			return null;
		}
		// bare names like "flight" are shorthand for "powers:flight"
		String normalized = idString.indexOf(':') < 0 ? PowersMod.MOD_ID + ":" + idString : idString;
		Identifier id = Identifier.tryParse(normalized);
		return id != null ? get(id) : null;
	}

	public static boolean contains(String idString) {
		return get(idString) != null;
	}

	/** draws count distinct random powers without repeats */
	public static List<Power> randomDistinct(int count, Random random) {
		List<Power> pool = new ArrayList<>(getAssignable());
		Collections.shuffle(pool, random);
		return pool.subList(0, Math.min(count, pool.size()));
	}

	/**
	 * Draws a compatible loadout and guarantees one allegiance-exclusive power
	 * whenever the requested count is non-zero.
	 */
	public static List<Power> randomDistinct(int count, Random random, PowerAffinity allegiance) {
		if (count <= 0) return List.of();
		List<Power> exclusive = getAssignable(allegiance).stream()
				.filter(power -> power.affinity() == allegiance)
				.collect(java.util.stream.Collectors.toCollection(ArrayList::new));
		List<Power> pool = new ArrayList<>(getAssignable(allegiance));
		Collections.shuffle(exclusive, random);
		Collections.shuffle(pool, random);
		List<Power> result = new ArrayList<>();
		if (!exclusive.isEmpty()) result.add(exclusive.getFirst());
		for (Power power : pool) {
			if (result.size() >= count) break;
			if (!result.contains(power)) result.add(power);
		}
		return List.copyOf(result);
	}

	/**
	 * Migrates an existing loadout after an allegiance change. Compatible,
	 * distinct slots are preserved; forbidden or missing slots are replaced.
	 */
	public static List<String> reconcile(List<String> existing, PowerAffinity allegiance) {
		int targetSize = Math.min(existing.size(), getAssignable(allegiance).size());
		List<Power> result = new ArrayList<>();
		for (String id : existing) {
			Power power = get(id);
			if (power != null && power.affinity().permits(allegiance) && !result.contains(power)) {
				result.add(power);
			}
			if (result.size() == targetSize) break;
		}

		String exclusiveId = allegiance == PowerAffinity.DARKNESS ? "energy_drain" : "starfall";
		for (String fallbackId : List.of("flight", "forcefield", exclusiveId)) {
			Power fallback = get(fallbackId);
			if (result.size() < targetSize && fallback != null && !result.contains(fallback)) {
				result.add(fallback);
			}
		}
		for (Power fallback : getAssignable(allegiance)) {
			if (result.size() >= targetSize) break;
			if (!result.contains(fallback)) result.add(fallback);
		}

		boolean hasExclusive = result.stream().anyMatch(power -> power.affinity() == allegiance);
		if (!hasExclusive && !result.isEmpty()) {
			Power exclusive = get(exclusiveId);
			if (exclusive != null) result.set(result.size() - 1, exclusive);
		}
		return result.stream().map(power -> power.id().toString()).toList();
	}

}

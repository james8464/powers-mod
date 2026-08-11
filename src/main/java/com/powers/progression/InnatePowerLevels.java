package com.powers.progression;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Authored rank shapes for every assignable innate power.
 *
 * <p>Each row deliberately defines the level-ten identity of one power rather
 * than applying one global percentage formula. Intermediate levels are stable
 * samples of that authored shape, with named transformations at levels 3, 6,
 * 9, and 10. This keeps all 253 supported power/level combinations complete
 * without a brittle copied table of nearly identical numbers.</p>
 */
public final class InnatePowerLevels {
	private static final InnatePowerLevel BASELINE = new InnatePowerLevel(
			1.0, 1.0, 1.0, 0, 1.0, Set.of());
	private static final Map<String, Shape> SHAPES = shapes();

	private InnatePowerLevels() {
	}

	/** Returns the complete immutable set of supported innate action IDs. */
	public static Set<String> powerIds() {
		return SHAPES.keySet();
	}

	/** Returns a finite profile; invalid levels clamp and unknown IDs stay unranked. */
	public static InnatePowerLevel forPower(String powerId, int requestedLevel) {
		if ("size_morph".equals(powerId)) powerId = "size_shift";
		Shape shape = SHAPES.get(powerId);
		if (shape == null) return BASELINE;
		int level = Math.clamp(requestedLevel, 0, 10);
		double progress = level / 10.0;
		Set<String> variants = new LinkedHashSet<>();
		if (level >= 3) addVariants(variants, shape.levelThree());
		if (level >= 6) addVariants(variants, shape.levelSix());
		if (level >= 9) addVariants(variants, shape.levelNine());
		if (level >= 10) addVariants(variants, shape.levelTen());
		return new InnatePowerLevel(
				interpolate(shape.maxDamage(), progress),
				interpolate(shape.maxRange(), progress),
				interpolate(shape.maxDuration(), progress),
				(int) Math.round(shape.maxDestruction() * progress),
				interpolate(shape.maxCapacity(), progress), variants);
	}

	private static double interpolate(double levelTen, double progress) {
		// Millesimal rounding makes saved/debug snapshots deterministic across JVMs.
		return Math.round((1.0 + (levelTen - 1.0) * progress) * 1_000.0) / 1_000.0;
	}

	private static void addVariants(Set<String> variants, String encoded) {
		for (String variant : encoded.split(",")) {
			if (!variant.isBlank()) variants.add(variant);
		}
	}

	private static Map<String, Shape> shapes() {
		Map<String, Shape> shapes = new LinkedHashMap<>();
		put(shapes, "size_shift", 1.8, 1.2, 1.8, 5, 3.0,
				"fine_control", "colossal_form", "density_mastery", "world_titan");
		put(shapes, "time_shift", 1.2, 4.0, 2.4, 0, 2.5,
				"distant_step", "dimensional_sense", "mass_transit", "world_stride");
		put(shapes, "flight", 2.0, 2.2, 4.0, 3, 3.0,
				"sonic_ascent,second_step", "storm_wake", "sky_dominion", "heavenbreaker");
		put(shapes, "starfall", 7.0, 3.2, 2.5, 10, 3.0,
				"astral_echo,empowered_impact", "moving_convergence,second_step,true_sight",
				"crowned_storm,soul_echo,afterimage,reflective_ward",
				"firmament_fall,ancient_mastery");
		put(shapes, "void_beam", 8.0, 3.2, 2.3, 10, 2.8,
				"piercing_void,empowered_impact", "abyssal_bore",
				"event_horizon,dark_resurgence", "world_rend,ancient_mastery");
		put(shapes, "fireball", 7.0, 2.6, 2.0, 10, 2.5,
				"cinderheart_charge,empowered_impact", "inferno_core,reflective_ward",
				"cataclysmic_reflection,afterimage,true_sight", "black_sun,ancient_mastery");
		put(shapes, "lightning_strike", 8.0, 3.2, 2.0, 10, 2.5,
				"forked_judgement,empowered_impact", "storm_chain,second_step",
				"sky_sentence,soul_echo,afterimage", "thunder_god,ancient_mastery,true_sight,reflective_ward");
		put(shapes, "thunderclap", 7.0, 3.6, 2.0, 10, 2.5,
				"pressure_front", "projectile_break", "mountain_echo", "world_clap");
		put(shapes, "speed_burst", 5.0, 3.0, 2.0, 7, 2.5,
				"second_step", "kinetic_wake", "barrier_break", "living_comet");
		put(shapes, "telekinesis", 5.0, 4.0, 2.4, 6, 4.0,
				"projectile_claim", "mass_lift", "army_release", "world_hand");
		put(shapes, "energy_beam", 8.0, 3.2, 2.6, 10, 3.5,
				"solar_flare,empowered_impact", "forked_ray", "sustained_sun",
				"daystar_lance,ancient_mastery");
		put(shapes, "super_speed", 4.0, 3.2, 3.5, 7, 3.0,
				"second_step", "memory_slip,afterimage", "chronal_pressure,empowered_impact",
				"time_runner,ancient_mastery");
		put(shapes, "breezy_bash", 6.0, 3.2, 2.5, 8, 4.0,
				"greater_capture,empowered_impact", "storm_apex", "mass_verdict",
				"tempest_sovereign,ancient_mastery");
		put(shapes, "invisibility", 1.0, 2.0, 4.0, 0, 4.0,
				"quiet_veil", "afterimage", "hostile_forgetting", "perfect_absence");
		put(shapes, "time_freeze", 1.5, 1.5, 2.5, 0, 4.0,
				"clock_sense", "frozen_projectiles", "contested_time", "eternal_instant");
		put(shapes, "forcefield", 2.0, 2.8, 3.0, 0, 8.0,
				"shared_shell", "reflective_ward", "siege_barrier", "absolute_aegis");
		put(shapes, "gravity_displacement", 6.0, 3.2, 2.8, 8, 5.0,
				"stable_orbit", "collapse,empowered_impact", "projectile_curvature",
				"singularity_court,ancient_mastery");
		put(shapes, "vessel_possession", 2.0, 4.0, 4.0, 0, 3.5,
				"creature_vessel", "deep_control", "shared_senses", "sovereign_mind");
		put(shapes, "astral_projection", 2.0, 4.0, 4.0, 0, 4.0,
				"astral_reach", "phase_sight", "soul_echo", "unbound_spirit");
		put(shapes, "energy_drain", 6.0, 3.2, 2.8, 4, 6.0,
				"deeper_well", "soul_echo", "mass_siphon", "endless_hunger");
		put(shapes, "ice_manipulation", 5.0, 3.2, 3.2, 8, 3.5,
				"deep_freeze", "glacial_path", "absolute_cold", "winter_crown");
		put(shapes, "plant_healing_acceleration", 4.0, 3.0, 3.2, 4, 6.0,
				"healing_bloom", "verdant_field", "mass_restoration", "worldspring");
		put(shapes, "double_health", 3.0, 1.2, 3.2, 0, 6.0,
				"vital_reserve", "regenerative_core", "death_defiance",
				"immortal_heart,ancient_mastery");
		return Map.copyOf(shapes);
	}

	private static void put(Map<String, Shape> target, String id,
			double damage, double range, double duration, int destruction, double capacity,
			String levelThree, String levelSix, String levelNine, String levelTen) {
		target.put(id, new Shape(damage, range, duration, destruction, capacity,
				levelThree, levelSix, levelNine, levelTen));
	}

	private record Shape(double maxDamage, double maxRange, double maxDuration,
			int maxDestruction, double maxCapacity, String levelThree, String levelSix,
			String levelNine, String levelTen) {
	}
}

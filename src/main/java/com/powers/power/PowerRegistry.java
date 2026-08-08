package com.powers.power;

import com.powers.PowersMod;
import com.powers.power.abilities.*;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Every randomly assigned innate power: an ability each, with passive
 * effects baked in. Drawing three random entries here is how a player
 * gets their powers, inspired by Rainbow Quest and classic superpower
 * mods like Superheroes Unlimited
 */
public final class PowerRegistry {
	// LinkedHashMap keeps registration order so the power list is stable
	private static final Map<Identifier, Power> POWERS = new LinkedHashMap<>();

	private PowerRegistry() {
	}

	public static void initialize() {
		register(new Power(PowersMod.id("slow_world"),
				Component.translatable("power.powers.slow_world"),
				Component.translatable("power.powers.slow_world.description"),
				0xFFD700,
				List.of(passive(MobEffects.HASTE, 0)),
				new SlowWorldAbility()));

		register(new Power(PowersMod.id("time_shift"),
				Component.translatable("power.powers.time_shift"),
				Component.translatable("power.powers.time_shift.description"),
				0xFFD700,
				List.of(passive(MobEffects.HASTE, 0)),
				new TeleportAbility()));

		register(new Power(PowersMod.id("shadow_step"),
				Component.translatable("power.powers.shadow_step"),
				Component.translatable("power.powers.shadow_step.description"),
				0x5E35B1,
				List.of(passive(MobEffects.NIGHT_VISION, 0)),
				new ShadowStepAbility()));

		register(new Power(PowersMod.id("flight"),
				Component.translatable("power.powers.flight"),
				Component.translatable("power.powers.flight.description"),
				0xFFFFFF,
				List.of(passive(MobEffects.SPEED, 0)),
				new FlightAbility()));

		register(new Power(PowersMod.id("elemental_blast"),
				Component.translatable("power.powers.elemental_blast"),
				Component.translatable("power.powers.elemental_blast.description"),
				0x00BCD4,
				List.of(passive(MobEffects.FIRE_RESISTANCE, 0)),
				new ElementalBlastAbility()));

		register(new Power(PowersMod.id("starfall"),
				Component.translatable("power.powers.starfall"),
				Component.translatable("power.powers.starfall.description"),
				0x3949AB,
				List.of(passive(MobEffects.HEALTH_BOOST, 0)),
				new StarfallAbility()));

		register(new Power(PowersMod.id("void_beam"),
				Component.translatable("power.powers.void_beam"),
				Component.translatable("power.powers.void_beam.description"),
				0x1A237E,
				List.of(passive(MobEffects.ABSORPTION, 0)),
				new VoidBeamAbility()));

		register(new Power(PowersMod.id("fireball"),
				Component.translatable("power.powers.fireball"),
				Component.translatable("power.powers.fireball.description"),
				0xFF4500,
				List.of(passive(MobEffects.FIRE_RESISTANCE, 0)),
				new FireballAbility()));

		register(new Power(PowersMod.id("frost_nova"),
				Component.translatable("power.powers.frost_nova"),
				Component.translatable("power.powers.frost_nova.description"),
				0x81D4FA,
				List.of(passive(MobEffects.WATER_BREATHING, 0)),
				new FrostNovaAbility()));

		register(new Power(PowersMod.id("lightning_strike"),
				Component.translatable("power.powers.lightning_strike"),
				Component.translatable("power.powers.lightning_strike.description"),
				0x4FC3F7,
				List.of(passive(MobEffects.RESISTANCE, 0)),
				new LightningStrikeAbility()));

		register(new Power(PowersMod.id("ground_slam"),
				Component.translatable("power.powers.ground_slam"),
				Component.translatable("power.powers.ground_slam.description"),
				0x4CAF50,
				List.of(passive(MobEffects.STRENGTH, 0)),
				new GroundSlamAbility()));

		register(new Power(PowersMod.id("speed_burst"),
				Component.translatable("power.powers.speed_burst"),
				Component.translatable("power.powers.speed_burst.description"),
				0xFFEB3B,
				List.of(passive(MobEffects.JUMP_BOOST, 1)),
				new SpeedBurstAbility()));

		register(new Power(PowersMod.id("telekinesis"),
				Component.translatable("power.powers.telekinesis"),
				Component.translatable("power.powers.telekinesis.description"),
				0x9C27B0,
				List.of(passive(MobEffects.SLOW_FALLING, 0)),
				new TelekinesisAbility()));

		register(new Power(PowersMod.id("energy_beam"),
				Component.translatable("power.powers.energy_beam"),
				Component.translatable("power.powers.energy_beam.description"),
				0xFF4500,
				List.of(passive(MobEffects.STRENGTH, 0)),
				new EnergyBeamAbility()));

		register(new Power(PowersMod.id("super_speed"),
				Component.translatable("power.powers.super_speed"),
				Component.translatable("power.powers.super_speed.description"),
				0x00E5FF,
				List.of(passive(MobEffects.SPEED, 1)),
				new SuperSpeedAbility()));

		register(new Power(PowersMod.id("breezy_bash"),
				Component.translatable("power.powers.breezy_bash"),
				Component.translatable("power.powers.breezy_bash.description"),
				0xB0BEC5,
				List.of(passive(MobEffects.SLOW_FALLING, 0)),
				new BreezyBashAbility()));

		register(new Power(PowersMod.id("cozy_campfire"),
				Component.translatable("power.powers.cozy_campfire"),
				Component.translatable("power.powers.cozy_campfire.description"),
				0xFFAB40,
				List.of(passive(MobEffects.REGENERATION, 0)),
				new CozyCampfireAbility()));

		register(new Power(PowersMod.id("invisibility"),
				Component.translatable("power.powers.invisibility"),
				Component.translatable("power.powers.invisibility.description"),
				0xAAAAAA,
				List.of(),
				new InvisibilityToggleAbility()));

		register(new Power(PowersMod.id("time_freeze"),
				Component.translatable("power.powers.time_freeze"),
				Component.translatable("power.powers.time_freeze.description"),
				0x80DEEA,
				List.of(passive(MobEffects.SPEED, 0)),
				new TimeFreezeToggleAbility()));

		register(new Power(PowersMod.id("forcefield"),
				Component.translatable("power.powers.forcefield"),
				Component.translatable("power.powers.forcefield.description"),
				0x40C4FF,
				List.of(passive(MobEffects.RESISTANCE, 0)),
				new ForcefieldAbility()));

		register(new Power(PowersMod.id("gravity_displacement"),
				Component.translatable("power.powers.gravity_displacement"),
				Component.translatable("power.powers.gravity_displacement.description"),
				0x7C4DFF,
				List.of(passive(MobEffects.SLOW_FALLING, 0)),
				new GravityDisplacementAbility()));

		register(new Power(PowersMod.id("vessel_possession"),
				Component.translatable("power.powers.vessel_possession"),
				Component.translatable("power.powers.vessel_possession.description"),
				0xE040FB,
				List.of(passive(MobEffects.NIGHT_VISION, 0)),
				new VesselPossessionAbility()));

		register(new Power(PowersMod.id("astral_projection"),
				Component.translatable("power.powers.astral_projection"),
				Component.translatable("power.powers.astral_projection.description"),
				0x7E57C2,
				List.of(passive(MobEffects.NIGHT_VISION, 0)),
				new AstralProjectionAbility()));

		register(new Power(PowersMod.id("energy_drain"),
				Component.translatable("power.powers.energy_drain"),
				Component.translatable("power.powers.energy_drain.description"),
				0x6A1B9A,
				List.of(),
				new EnergyDrainAbility()));

		register(new Power(PowersMod.id("ice_manipulation"),
				Component.translatable("power.powers.ice_manipulation"),
				Component.translatable("power.powers.ice_manipulation.description"),
				0x81D4FA,
				List.of(passive(MobEffects.WATER_BREATHING, 0)),
				new IceManipulationAbility()));

		register(new Power(PowersMod.id("plant_healing_acceleration"),
				Component.translatable("power.powers.plant_healing_acceleration"),
				Component.translatable("power.powers.plant_healing_acceleration.description"),
				0x66FF66,
				List.of(passive(MobEffects.REGENERATION, 0)),
				new PlantHealingAbility()));

		register(new Power(PowersMod.id("double_health"),
				Component.translatable("power.powers.double_health"),
				Component.translatable("power.powers.double_health.description"),
				0xFF1744,
				List.of(),
				new DoubleHealthAbility()));
	}

	private static PassiveEffect passive(Holder<MobEffect> effect, int amplifier) {
		return new PassiveEffect(effect, amplifier);
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
}

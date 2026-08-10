package com.powers.fx;

import com.powers.PowersParticles;
import com.powers.PowersSounds;
import com.powers.power.abilities.EnergyBeamRules;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/** Ancient solar choreography dedicated to the channeled Sunfire Energy Beam. */
public final class EnergyBeamFx {
	private static final int SUN_GOLD = 0xFFD166;
	private static final int SUN_WHITE = 0xFFF4D6;
	private static final int EMBER = 0xFF6B1A;

	private EnergyBeamFx() {
	}

	/** Focuses counter-rotating solar seals around the caster's live eye position. */
	public static void focus(ServerLevel level, Vec3 eye, int remainingTicks,
			boolean ancientMastery) {
		double phase = level.getGameTime() * 0.20;
		double radius = 0.34 + (8 - Math.clamp(remainingTicks, 0, 8)) * 0.035;
		PowerFx.ring(level, eye, radius, SUN_GOLD, ancientMastery ? 18 : 14, phase);
		PowerFx.ring(level, eye, radius * 0.66, SUN_WHITE,
				ancientMastery ? 14 : 10, -phase * 1.45);
		PowerFx.burst(level, eye, PowersParticles.GLYPH,
				ancientMastery ? 7 : 4, radius * 0.55, 0.025);
		PowerFx.burst(level, eye, ParticleTypes.FLAME,
				ancientMastery ? 5 : 3, radius * 0.32, 0.015);
		if (remainingTicks == 8) {
			PowerFx.rune(level, eye.subtract(0.0, 1.55, 0.0), ancientMastery ? 1.25 : 0.95,
					SUN_GOLD, ancientMastery ? 28 : 20, phase);
			PowerFx.sound(level, eye, PowersSounds.RUNE_HUM, ancientMastery ? 1.1F : 0.8F, 1.26F);
			PowerFx.sound(level, eye, SoundEvents.BEACON_POWER_SELECT, 0.75F, 1.72F);
		}
	}

	/** Draws the live-aim solar lance; damage beats carry a brighter core. */
	public static void ray(ServerLevel level, Vec3 from, Vec3 to,
			boolean damageBeat, boolean ancientMastery) {
		double length = from.distanceTo(to);
		int steps = Math.clamp((int) Math.ceil(length * 0.55), 6, ancientMastery ? 38 : 30);
		PowerFx.beam(level, from, to, PowersParticles.RIBBON, steps);
		PowerFx.beam(level, from, to, PowerFx.dust(0xFFD166, ancientMastery ? 1.3F : 0.9F),
				Math.max(5, steps / 2));
		if (damageBeat) {
			PowerFx.beam(level, from, to, ParticleTypes.FLAME, Math.max(6, steps / 2));
			PowerFx.burst(level, from, PowersParticles.SPARK,
					ancientMastery ? 13 : 9, 0.28, 0.12);
			PowerFx.sound(level, from, SoundEvents.BLAZE_SHOOT, 0.92F, 1.42F);
		}
	}

	/** Marks a successful scorch and makes repeated hits visibly escalate. */
	public static void impact(ServerLevel level, Vec3 point, int streak) {
		int tier = Math.clamp(streak, 1, 3);
		PowerFx.ring(level, point, 0.48 + tier * 0.14, SUN_GOLD,
				12 + tier * 4, level.getGameTime() * 0.17);
		PowerFx.burst(level, point, ParticleTypes.FLAME, 10 + tier * 6,
				0.26 + tier * 0.10, 0.10 + tier * 0.035);
		PowerFx.burst(level, point, PowersParticles.SPARK, 6 + tier * 4,
				0.24 + tier * 0.08, 0.10);
		PowerFx.coloredBurst(level, point, tier == 3 ? SUN_WHITE : EMBER,
				7 + tier * 3, 0.28 + tier * 0.08);
		PowerFx.sound(level, point, SoundEvents.FIRECHARGE_USE, 0.65F, 1.05F + tier * 0.12F);
	}

	/** Transforms a water-grounded damage beat into a readable steam pressure bloom. */
	public static void steam(ServerLevel level, Vec3 point) {
		PowerFx.rune(level, point.add(0.0, 0.08, 0.0), 1.15, 0xBDEFFF,
				24, level.getGameTime() * 0.11);
		PowerFx.burst(level, point, ParticleTypes.CLOUD, 42, 1.35, 0.24);
		PowerFx.burst(level, point, ParticleTypes.END_ROD, 18, 0.86, 0.12);
		PowerFx.burst(level, point, PowersParticles.FRACTURE, 14, 0.72, 0.08);
		PowerFx.sound(level, point, SoundEvents.FIRE_EXTINGUISH, 1.2F, 0.72F);
		PowerFx.sound(level, point, PowersSounds.INTERACTION_CLASH, 0.8F, 1.32F);
	}

	/** Shows one Ancient Mastery fork without implying recursive chaining. */
	public static void split(ServerLevel level, Vec3 from, Vec3 to, int index) {
		PowerFx.beam(level, from, to, index == 0 ? PowersParticles.SPARK : PowersParticles.RIBBON,
				10);
		PowerFx.beam(level, from, to, PowerFx.dust(index == 0 ? 0xFFF4D6 : 0xFFB347, 1.0F), 7);
		PowerFx.ring(level, to, 0.5, index == 0 ? SUN_WHITE : SUN_GOLD,
				12, level.getGameTime() * 0.15);
		PowerFx.burst(level, to, ParticleTypes.FLAME, 8, 0.3, 0.08);
		PowerFx.sound(level, to, SoundEvents.FIRECHARGE_USE, 0.42F, 1.55F + index * 0.14F);
	}

	/** Erupts the single Empowered Impact flare as a bounded solar corona. */
	public static void flare(ServerLevel level, Vec3 point) {
		PowerFx.rune(level, point.add(0.0, 0.06, 0.0), 2.6, SUN_GOLD,
				38, level.getGameTime() * 0.12);
		PowerFx.ring(level, point.add(0.0, 0.7, 0.0), 2.0, SUN_WHITE,
				32, -level.getGameTime() * 0.16);
		PowerFx.burst(level, point, net.minecraft.core.particles.ColorParticleOption.create(
				ParticleTypes.FLASH, 0xFFFFF4D6), 4, 0.34, 0.0);
		PowerFx.burst(level, point, ParticleTypes.FLAME, 52, 2.1, 0.28);
		PowerFx.burst(level, point, PowersParticles.FRACTURE, 28, 1.55, 0.20);
		PowerFx.sound(level, point, SoundEvents.GENERIC_EXPLODE.value(), 1.35F, 1.26F);
		PowerFx.sound(level, point, PowersSounds.LIGHT_CHORUS, 1.1F, 1.48F);
	}

	/** Gives each terminal counter a distinct ancient-material response. */
	public static void countered(ServerLevel level, Vec3 point,
			EnergyBeamRules.Counterplay counterplay) {
		if (counterplay == null) return;
		int primary = switch (counterplay) {
			case SURFACE -> 0xD96B2B;
			case WATER -> 0xBDEFFF;
			case AMETHYST -> 0xB36BFF;
			case PURE_LIGHT -> 0xFFF4D6;
			case DARKNESS -> 0x4A185F;
			case KINETIC_WARD -> 0x70D6FF;
			case SANCTUARY -> 0x8CFF98;
			case FORCEFIELD -> 0x40C4FF;
			case SAFE_ZONE -> 0x58C7FF;
			case RESISTED -> 0xB9A98A;
		};
		int secondary = switch (counterplay) {
			case SURFACE -> EMBER;
			case WATER, KINETIC_WARD, FORCEFIELD, SAFE_ZONE -> SUN_WHITE;
			case AMETHYST -> 0x5E2A84;
			case PURE_LIGHT -> SUN_GOLD;
			case DARKNESS -> 0x14051F;
			case SANCTUARY -> 0xFFE8A3;
			case RESISTED -> 0x665C4B;
		};
		PowerFx.ring(level, point, 0.92, primary, 18, level.getGameTime() * 0.14);
		PowerFx.ring(level, point.add(0.0, 0.10, 0.0), 0.58, secondary,
				14, -level.getGameTime() * 0.19);
		PowerFx.burst(level, point,
				counterplay == EnergyBeamRules.Counterplay.AMETHYST
						? ParticleTypes.ELECTRIC_SPARK : PowersParticles.FRACTURE,
				14, 0.52, 0.09);
		if (counterplay == EnergyBeamRules.Counterplay.DARKNESS) {
			PowerFx.burst(level, point, ParticleTypes.REVERSE_PORTAL, 18, 0.58, 0.08);
		}
		PowerFx.sound(level, point, switch (counterplay) {
			case AMETHYST -> PowersSounds.AMETHYST_FRACTURE;
			case PURE_LIGHT -> PowersSounds.LIGHT_CHORUS;
			case DARKNESS -> PowersSounds.DARK_WHISPER;
			case SURFACE -> SoundEvents.FIRECHARGE_USE;
			case WATER -> SoundEvents.FIRE_EXTINGUISH;
			case KINETIC_WARD, SANCTUARY, FORCEFIELD, SAFE_ZONE -> PowersSounds.WARD_IMPACT;
			case RESISTED -> SoundEvents.BEACON_DEACTIVATE;
		}, 0.82F, counterplay == EnergyBeamRules.Counterplay.DARKNESS ? 0.62F : 1.08F);
	}

	/** Collapses a cancelled channel into a broken solar seal. */
	public static void interrupted(ServerLevel level, Vec3 point, boolean amethyst) {
		int color = amethyst ? 0xB36BFF : 0xD96B2B;
		PowerFx.ring(level, point, 0.72, color, 18, level.getGameTime() * 0.2);
		PowerFx.burst(level, point, PowersParticles.FRACTURE, 20, 0.72, 0.13);
		PowerFx.burst(level, point, ParticleTypes.SMOKE, 14, 0.44, 0.08);
		PowerFx.sound(level, point,
				amethyst ? PowersSounds.AMETHYST_FRACTURE : SoundEvents.BEACON_DEACTIVATE,
				0.86F, amethyst ? 0.92F : 0.72F);
	}

	/** Seals a naturally completed channel with a quiet descending corona. */
	public static void complete(ServerLevel level, Vec3 point, boolean ancientMastery) {
		PowerFx.ring(level, point, ancientMastery ? 0.82 : 0.62, SUN_GOLD,
				ancientMastery ? 22 : 16, -level.getGameTime() * 0.18);
		PowerFx.burst(level, point, ParticleTypes.END_ROD,
				ancientMastery ? 14 : 9, 0.46, 0.07);
		PowerFx.sound(level, point, SoundEvents.BEACON_DEACTIVATE, 0.46F, 1.52F);
	}
}

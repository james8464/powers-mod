package com.powers.fx;

import com.powers.PowersParticles;
import com.powers.PowersSounds;
import com.powers.power.abilities.StarfallRules;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/** Gold-indigo celestial choreography dedicated to Astral Convergence. */
public final class StarfallFx {
	private static final int INDIGO = 0x3949AB;
	private static final int DEEP_INDIGO = 0x151A4F;
	private static final int GOLD = 0xFFF2B0;
	private static final int WHITE = 0xFFFDF0;
	private static final int CYAN = 0x8FE9FF;
	private static final int WATER = 0x69D5FF;

	private StarfallFx() {
	}

	/** Opens the complete astrolabe footprint before any damaging beat. */
	public static void open(ServerLevel level, Vec3 center, double radius,
			int strikes, boolean tracking, boolean ancientMastery) {
		PowerFx.rune(level, center, radius, INDIGO, 40,
				level.getGameTime() * 0.06);
		PowerFx.ring(level, center.add(0.0, 0.12, 0.0), radius * 0.72,
				GOLD, 32, -level.getGameTime() * 0.09);
		PowerFx.ring(level, center.add(0.0, 0.24, 0.0), radius * 0.38,
				tracking ? CYAN : WHITE, 24, Math.PI / 8.0);
		PowerFx.spiral(level, center, radius * 0.22, 7.0, GOLD,
				18 + strikes, 0.0);
		PowerFx.burst(level, center.add(0.0, 7.0, 0.0), PowersParticles.GLYPH,
				12 + strikes, Math.min(3.0, radius * 0.4), 0.05);
		if (ancientMastery) {
			PowerFx.burst(level, center.add(0.0, 9.0, 0.0), ParticleTypes.END_ROD,
					18, Math.min(2.4, radius * 0.32), 0.035);
		}
		PowerFx.sound(level, center, SoundEvents.END_PORTAL_SPAWN, 1.05F, 1.42F);
		PowerFx.sound(level, center, PowersSounds.LIGHT_CHORUS, 0.72F,
				ancientMastery ? 0.76F : 1.08F);
		PowerFx.sound(level, center, PowersSounds.RUNE_HUM, 0.52F, 0.68F);
	}

	/** Sustains a sparse rotating omen clock during the one-second warning. */
	public static void omen(ServerLevel level, Vec3 center, double radius,
			int age, boolean afterimage, boolean ancientMastery) {
		double progress = Math.max(0.0, Math.min(1.0, age / 20.0));
		double phase = age * 0.17;
		PowerFx.ring(level, center, radius, INDIGO, 28, phase);
		PowerFx.ring(level, center.add(0.0, 0.1, 0.0),
				radius * (0.84 - progress * 0.34), GOLD, 22, -phase * 1.4);
		PowerFx.rune(level, center.add(0.0, 0.18, 0.0),
				0.8 + progress * 1.4, ancientMastery ? WHITE : GOLD,
				16 + (int) (progress * 12), phase * 0.6);
		PowerFx.burst(level, center.add(0.0, 5.0 + progress * 5.0, 0.0),
				afterimage ? PowersParticles.RIBBON : PowersParticles.MOTE,
				3 + (int) (progress * 4), radius * 0.18, 0.025);
		if (age == 10) PowerFx.sound(level, center, PowersSounds.RUNE_HUM, 0.65F, 0.88F);
		if (age == 18) PowerFx.sound(level, center, SoundEvents.BEACON_POWER_SELECT, 0.82F, 1.78F);
	}

	/** Shows a specific impact point for its final three ticks of counterplay. */
	public static void telegraph(ServerLevel level, Vec3 point, int ticksUntil,
			int index, boolean crown) {
		double progress = 1.0 - Math.max(0, ticksUntil - 1) / 3.0;
		double radius = crown ? 2.4 + progress * 1.6 : 0.65 + progress * 0.85;
		int color = crown ? WHITE : index % 2 == 0 ? GOLD : CYAN;
		PowerFx.rune(level, point, radius, color, crown ? 36 : 18,
				level.getGameTime() * (crown ? 0.2 : 0.32));
		PowerFx.ring(level, point.add(0.0, 0.09, 0.0), radius * 0.56,
				DEEP_INDIGO, crown ? 28 : 14, -level.getGameTime() * 0.26);
		PowerFx.beam(level, point.add(0.0, 12.0, 0.0), point.add(0.0, 0.2, 0.0),
				ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,
						0xFF000000 | color), crown ? 26 : 14);
		PowerFx.burst(level, point.add(0.0, 0.2, 0.0), PowersParticles.GLYPH,
				crown ? 8 : 3, radius * 0.28, 0.02);
		if (ticksUntil == 1) {
			PowerFx.sound(level, point, SoundEvents.RESPAWN_ANCHOR_CHARGE,
					crown ? 1.2F : 0.62F, crown ? 1.45F : 1.72F);
		}
	}

	/** Draws one ordinary celestial descent with visible sky-to-surface direction. */
	public static void strike(ServerLevel level, Vec3 sky, Vec3 point, double radius,
			int index, int affected, boolean empoweredImpact) {
		PowerFx.beam(level, sky, point, ParticleTypes.ELECTRIC_SPARK, 30);
		PowerFx.beam(level, sky.add(0.18, 0.0, -0.12), point,
				PowersParticles.RIBBON, 24);
		PowerFx.rune(level, point, radius, empoweredImpact ? WHITE : GOLD,
				28, index * Math.PI / 5.0);
		PowerFx.ring(level, point.add(0.0, 0.12, 0.0), radius * 0.66,
				INDIGO, 22, -index * Math.PI / 7.0);
		PowerFx.burst(level, point.add(0.0, 0.6, 0.0), ParticleTypes.ELECTRIC_SPARK,
				22 + Math.min(18, affected * 3), Math.min(1.8, radius * 0.48), 0.22);
		PowerFx.burst(level, point.add(0.0, 0.5, 0.0), PowersParticles.FRACTURE,
				12 + Math.min(12, affected * 2), Math.min(1.5, radius * 0.4), 0.12);
		PowerFx.sound(level, point, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2F,
				0.92F + Math.min(0.25F, index * 0.025F));
		PowerFx.sound(level, point, PowersSounds.INTERACTION_CLASH, 0.62F, 1.46F);
	}

	/** Converts a strike into a wider, lower-damage conductive water sigil. */
	public static void conduction(ServerLevel level, Vec3 sky, Vec3 point,
			double radius, int affected) {
		PowerFx.beam(level, sky, point, ParticleTypes.ELECTRIC_SPARK, 26);
		PowerFx.rune(level, point, radius, WATER, 34,
				level.getGameTime() * 0.2);
		PowerFx.ring(level, point.add(0.0, 0.12, 0.0), radius * 0.72,
				WHITE, 28, -level.getGameTime() * 0.28);
		PowerFx.burst(level, point.add(0.0, 0.35, 0.0), ParticleTypes.SPLASH,
				24 + Math.min(16, affected * 2), radius * 0.42, 0.18);
		PowerFx.burst(level, point.add(0.0, 0.45, 0.0), ParticleTypes.ELECTRIC_SPARK,
				20 + Math.min(12, affected * 2), radius * 0.34, 0.20);
		PowerFx.sound(level, point, SoundEvents.TRIDENT_THUNDER.value(), 1.15F, 1.18F);
		PowerFx.sound(level, point, PowersSounds.INTERACTION_CLASH, 0.72F, 1.52F);
	}

	/** Connects a Communion strike to its one mirrored, reduced echo. */
	public static void echo(ServerLevel level, Vec3 first, Vec3 echo, int index) {
		PowerFx.beam(level, first.add(0.0, 0.4, 0.0), echo.add(0.0, 0.4, 0.0),
				PowersParticles.RIBBON, 18);
		PowerFx.rune(level, echo, 1.0, CYAN, 18,
				index * Math.PI / 3.0);
		PowerFx.burst(level, echo.add(0.0, 0.35, 0.0), PowersParticles.MOTE,
				14, 0.65, 0.08);
		PowerFx.sound(level, echo, PowersSounds.SOUL_TETHER, 0.58F, 1.48F);
	}

	/** Shows Pure Light amplifying the strike without changing either block. */
	public static void resonance(ServerLevel level, Vec3 point, double radius) {
		PowerFx.rune(level, point, radius, WHITE, 36,
				level.getGameTime() * 0.22);
		PowerFx.ring(level, point.add(0.0, 0.16, 0.0), radius * 0.68,
				GOLD, 30, -level.getGameTime() * 0.31);
		PowerFx.burst(level, point.add(0.0, 0.8, 0.0), ParticleTypes.END_ROD,
				28, radius * 0.38, 0.16);
		PowerFx.sound(level, point, PowersSounds.LIGHT_CHORUS, 1.15F, 1.32F);
	}

	/** Gives every blocked impact a semantic double seal and sound. */
	public static void terminal(ServerLevel level, Vec3 point,
			StarfallRules.Counterplay counterplay) {
		if (counterplay == null || counterplay == StarfallRules.Counterplay.STRIKE
				|| counterplay == StarfallRules.Counterplay.WATER
				|| counterplay == StarfallRules.Counterplay.PURE_LIGHT) return;
		int color = switch (counterplay) {
			case UNOWNED, UNLOADED, ROOF, RESISTED -> 0x8A8F96;
			case SAFE_ZONE -> 0x58C7FF;
			case AMETHYST -> 0xB36BFF;
			case SANCTUARY -> 0x8CFF98;
			case KINETIC_WARD, FORCEFIELD -> 0x40C4FF;
			case DARKNESS -> 0x2A0C3D;
			case TIME_LOCK -> 0xE8FFFF;
			case BODY_ANCHOR -> 0xBCA7FF;
			case STRIKE, WATER, PURE_LIGHT -> GOLD;
		};
		PowerFx.rune(level, point, 1.15, color, 22,
				level.getGameTime() * 0.14);
		PowerFx.ring(level, point.add(0.0, 0.12, 0.0), 0.72,
				WHITE, 18, -level.getGameTime() * 0.21);
		PowerFx.burst(level, point, counterplay == StarfallRules.Counterplay.AMETHYST
				? ParticleTypes.ELECTRIC_SPARK : PowersParticles.FRACTURE,
				18, 0.58, 0.10);
		PowerFx.sound(level, point, switch (counterplay) {
			case AMETHYST -> PowersSounds.AMETHYST_FRACTURE;
			case DARKNESS -> PowersSounds.DARK_WHISPER;
			case TIME_LOCK -> PowersSounds.TIME_SUSPEND;
			case BODY_ANCHOR -> PowersSounds.SOUL_TETHER;
			case SAFE_ZONE, SANCTUARY, KINETIC_WARD, FORCEFIELD -> PowersSounds.WARD_IMPACT;
			case UNOWNED, UNLOADED, ROOF, RESISTED -> SoundEvents.BEACON_DEACTIVATE;
			case STRIKE, WATER, PURE_LIGHT -> PowersSounds.INTERACTION_CLASH;
		}, 0.78F, counterplay == StarfallRules.Counterplay.DARKNESS ? 0.58F : 0.92F);
	}

	/** Marks Insight tearing through a power veil and naming the hidden body. */
	public static void revelation(ServerLevel level, Vec3 point) {
		PowerFx.rune(level, point, 0.92, CYAN, 20, Math.PI / 4.0);
		PowerFx.ring(level, point.add(0.0, 0.16, 0.0), 0.55,
				GOLD, 16, -Math.PI / 4.0);
		PowerFx.burst(level, point, PowersParticles.GLYPH, 14, 0.48, 0.06);
		PowerFx.burst(level, point, ParticleTypes.END_ROD, 10, 0.38, 0.08);
		PowerFx.sound(level, point, SoundEvents.AMETHYST_BLOCK_CHIME, 0.82F, 1.72F);
	}

	/** Releases Dominion's final central crown as a three-layer stellar verdict. */
	public static void crown(ServerLevel level, Vec3 sky, Vec3 point,
			double radius, int affected) {
		PowerFx.beam(level, sky, point, ParticleTypes.ELECTRIC_SPARK, 42);
		PowerFx.beam(level, sky.add(0.3, 0.0, 0.3), point,
				PowersParticles.RIBBON, 36);
		PowerFx.rune(level, point, radius, WHITE, 52,
				level.getGameTime() * 0.18);
		PowerFx.rune(level, point.add(0.0, 0.16, 0.0), radius * 0.72,
				GOLD, 44, -level.getGameTime() * 0.25);
		PowerFx.ring(level, point.add(0.0, 0.32, 0.0), radius * 0.42,
				INDIGO, 34, level.getGameTime() * 0.34);
		PowerFx.burst(level, point.add(0.0, 0.8, 0.0),
				ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), 3, 0.8, 0.0);
		PowerFx.burst(level, point.add(0.0, 0.9, 0.0), ParticleTypes.ELECTRIC_SPARK,
				48 + Math.min(24, affected * 3), radius * 0.46, 0.34);
		PowerFx.burst(level, point.add(0.0, 0.9, 0.0), PowersParticles.GLYPH,
				28, radius * 0.34, 0.14);
		PowerFx.sound(level, point, SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0F, 0.68F);
		PowerFx.sound(level, point, SoundEvents.WARDEN_SONIC_BOOM, 1.25F, 1.34F);
		PowerFx.sound(level, point, PowersSounds.LIGHT_CHORUS, 1.35F, 0.74F);
	}

	/** Shows Motion dragging the storm eye without hiding its authoritative path. */
	public static void tracking(ServerLevel level, Vec3 from, Vec3 to, int age) {
		if (from.distanceToSqr(to) <= 1.0E-6) return;
		PowerFx.beam(level, from.add(0.0, 0.18, 0.0), to.add(0.0, 0.18, 0.0),
				PowersParticles.RIBBON, 8);
		PowerFx.ring(level, to, 0.62, CYAN, 12, age * 0.2);
	}

	/** Marks Wardcraft curving one hostile projectile without stealing ownership. */
	public static void wardProjectile(ServerLevel level, Vec3 center,
			Vec3 projectile, int age, int index) {
		PowerFx.beam(level, center.add(0.0, 0.3, 0.0), projectile,
				PowersParticles.RIBBON, 8);
		PowerFx.rune(level, projectile, 0.42 + index * 0.018, CYAN,
				10, age * 0.24 + index);
		PowerFx.burst(level, projectile, ParticleTypes.ELECTRIC_SPARK,
				5, 0.18, 0.06);
		if (index == 0) {
			PowerFx.sound(level, projectile, PowersSounds.WARD_IMPACT, 0.45F, 1.58F);
		}
	}

	/** Collapses completion or interruption without leaving a damaging residue. */
	public static void collapse(ServerLevel level, Vec3 center, double radius,
			boolean completed, boolean amethyst, boolean frozen) {
		int color = amethyst ? 0xB36BFF : frozen ? 0xE8FFFF : completed ? GOLD : 0x8A8F96;
		PowerFx.rune(level, center, Math.min(3.8, radius * 0.6), color,
				completed ? 34 : 24, Math.PI);
		PowerFx.ring(level, center.add(0.0, 0.12, 0.0),
				Math.min(2.6, radius * 0.4), DEEP_INDIGO, completed ? 28 : 18, -Math.PI);
		PowerFx.burst(level, center.add(0.0, 0.5, 0.0), amethyst
				? ParticleTypes.ELECTRIC_SPARK : completed
						? ParticleTypes.REVERSE_PORTAL : PowersParticles.FRACTURE,
				completed ? 32 : 18, Math.min(1.8, radius * 0.28), 0.10);
		PowerFx.sound(level, center, amethyst ? PowersSounds.AMETHYST_FRACTURE
				: frozen ? PowersSounds.TIME_SUSPEND
						: completed ? PowersSounds.LIGHT_CHORUS : SoundEvents.BEACON_DEACTIVATE,
				completed ? 0.9F : 0.68F, completed ? 1.28F : 0.78F);
	}

	/** Refuses an invalid or protected initial field before payment remains committed. */
	public static void blocked(ServerLevel level, Vec3 point,
			StarfallRules.Counterplay counterplay) {
		terminal(level, point, counterplay == StarfallRules.Counterplay.STRIKE
				? StarfallRules.Counterplay.RESISTED : counterplay);
	}
}

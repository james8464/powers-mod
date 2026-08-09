package com.powers.fx;

import com.powers.PowersParticles;
import com.powers.PowersSounds;
import com.powers.power.abilities.LightningStrikeRules;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/** Cyan-gold storm choreography dedicated to the finite Storm Tribunal. */
public final class LightningStrikeFx {
	private static final int CYAN = 0x70E7FF;
	private static final int DEEP_CYAN = 0x146C8E;
	private static final int GOLD = 0xFFE58A;
	private static final int WHITE = 0xF5FFFF;
	private static final int STORM_BLUE = 0x355DCC;
	private static final int WATER_BLUE = 0x57C7FF;

	private LightningStrikeFx() {
	}

	/** Opens the eight-spoked storm compass before any damaging beat. */
	public static void open(ServerLevel level, Vec3 sky, Vec3 point,
			double radius, boolean tracking, boolean soulEcho,
			boolean ancientMastery) {
		PowerFx.rune(level, point, Math.max(1.5, radius * 0.88), CYAN,
				ancientMastery ? 40 : 32, level.getGameTime() * 0.10);
		PowerFx.ring(level, point.add(0.0, 0.10, 0.0), radius * 0.58,
				GOLD, soulEcho ? 28 : 22, -level.getGameTime() * 0.15);
		PowerFx.ring(level, point.add(0.0, 0.20, 0.0), radius * 0.28,
				tracking ? WHITE : STORM_BLUE, 16, Math.PI / 8.0);
		PowerFx.beam(level, sky, point.add(0.0, 0.2, 0.0),
				ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,
						0xFF000000 | DEEP_CYAN), 18);
		PowerFx.spiral(level, point.add(0.0, 0.15, 0.0),
				radius * 0.24, 4.0, GOLD, ancientMastery ? 26 : 18, 0.0);
		PowerFx.burst(level, point.add(0.0, 0.35, 0.0), PowersParticles.GLYPH,
				ancientMastery ? 16 : 10, radius * 0.24, 0.04);
		PowerFx.sound(level, point, PowersSounds.RUNE_HUM, 0.72F, 1.24F);
		PowerFx.sound(level, point, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.60F, 1.46F);
	}

	/** Contracts the compass and brightens its sky filament through eight warning ticks. */
	public static void omen(ServerLevel level, Vec3 sky, Vec3 point,
			double radius, int age, boolean afterimage, boolean ancientMastery) {
		double progress = Math.max(0.0, Math.min(1.0, age / 8.0));
		double phase = age * 0.31;
		PowerFx.ring(level, point, radius * (0.92 - progress * 0.42),
				CYAN, 24, phase);
		PowerFx.rune(level, point.add(0.0, 0.12, 0.0),
				0.65 + progress * 1.0, ancientMastery ? WHITE : GOLD,
				18 + (int) (progress * 10), -phase * 1.35);
		PowerFx.beam(level, sky, point.add(0.0, 0.22, 0.0),
				afterimage ? PowersParticles.RIBBON : ParticleTypes.ELECTRIC_SPARK,
				12 + age);
		PowerFx.burst(level, point.add(0.0, 0.5 + progress * 1.2, 0.0),
				afterimage ? PowersParticles.RIBBON : PowersParticles.SPARK,
				3 + (int) (progress * 4), radius * 0.17, 0.05);
		if (age == 4) PowerFx.sound(level, point, PowersSounds.RUNE_HUM, 0.48F, 1.48F);
		if (age == 7) PowerFx.sound(level, point,
				SoundEvents.BEACON_POWER_SELECT, 0.82F, 1.92F);
	}

	/** Shows the exact authoritative endpoint during the final three counterplay ticks. */
	public static void telegraph(ServerLevel level, Vec3 sky, Vec3 point,
			int ticksUntil, LightningStrikeRules.Beat beat) {
		double progress = 1.0 - Math.max(0, ticksUntil - 1) / 3.0;
		double radius = beat == LightningStrikeRules.Beat.CROWN
				? 2.4 + progress * 1.4 : 0.65 + progress * 0.95;
		int color = beat == LightningStrikeRules.Beat.CROWN ? WHITE : GOLD;
		PowerFx.rune(level, point, radius, color,
				beat == LightningStrikeRules.Beat.CROWN ? 38 : 22,
				level.getGameTime() * 0.34);
		PowerFx.ring(level, point.add(0.0, 0.10, 0.0), radius * 0.58,
				DEEP_CYAN, beat == LightningStrikeRules.Beat.CROWN ? 30 : 16,
				-level.getGameTime() * 0.42);
		PowerFx.beam(level, sky, point.add(0.0, 0.2, 0.0),
				ParticleTypes.ELECTRIC_SPARK,
				beat == LightningStrikeRules.Beat.CROWN ? 30 : 18);
		PowerFx.burst(level, point.add(0.0, 0.3, 0.0), PowersParticles.GLYPH,
				beat == LightningStrikeRules.Beat.CROWN ? 10 : 5,
				radius * 0.25, 0.025);
		if (ticksUntil == 1) {
			PowerFx.sound(level, point, SoundEvents.RESPAWN_ANCHOR_CHARGE,
					beat == LightningStrikeRules.Beat.CROWN ? 1.2F : 0.68F,
					beat == LightningStrikeRules.Beat.CROWN ? 1.28F : 1.84F);
		}
	}

	/** Releases one layered verdict with material- and rank-specific signatures. */
	public static void impact(ServerLevel level, Vec3 sky, Vec3 point,
			double radius, LightningStrikeRules.Beat beat,
			LightningStrikeRules.Counterplay medium, int affected,
			boolean empoweredImpact) {
		int primary = switch (medium) {
			case WATER -> WATER_BLUE;
			case PURE_LIGHT -> WHITE;
			case ROOF -> 0xB7C5CE;
			default -> empoweredImpact ? WHITE : CYAN;
		};
		int secondary = medium == LightningStrikeRules.Counterplay.PURE_LIGHT
				? GOLD : medium == LightningStrikeRules.Counterplay.ROOF
						? 0x687984 : STORM_BLUE;
		int extra = beat == LightningStrikeRules.Beat.CROWN ? 18 : 0;
		PowerFx.beam(level, sky, point, ParticleTypes.ELECTRIC_SPARK, 34 + extra);
		PowerFx.beam(level, sky.add(0.20, 0.0, -0.14), point,
				PowersParticles.RIBBON, 26 + extra);
		PowerFx.rune(level, point, radius, primary, 34 + extra,
				level.getGameTime() * 0.23);
		PowerFx.ring(level, point.add(0.0, 0.13, 0.0), radius * 0.68,
				secondary, 28 + extra, -level.getGameTime() * 0.31);
		PowerFx.ring(level, point.add(0.0, 0.25, 0.0), radius * 0.36,
				GOLD, 20 + extra, level.getGameTime() * 0.44);
		PowerFx.burst(level, point.add(0.0, 0.75, 0.0), ParticleTypes.ELECTRIC_SPARK,
				30 + extra + Math.min(20, affected * 3), radius * 0.42, 0.28);
		PowerFx.burst(level, point.add(0.0, 0.55, 0.0), PowersParticles.FRACTURE,
				18 + extra + Math.min(12, affected * 2), radius * 0.34, 0.15);
		if (medium == LightningStrikeRules.Counterplay.WATER) {
			PowerFx.burst(level, point.add(0.0, 0.35, 0.0), ParticleTypes.SPLASH,
					28, radius * 0.45, 0.18);
		}
		if (medium == LightningStrikeRules.Counterplay.PURE_LIGHT) {
			PowerFx.burst(level, point.add(0.0, 0.8, 0.0), ParticleTypes.END_ROD,
					28, radius * 0.38, 0.16);
			PowerFx.sound(level, point, PowersSounds.LIGHT_CHORUS, 1.1F, 1.42F);
		}
		PowerFx.sound(level, point, SoundEvents.LIGHTNING_BOLT_THUNDER,
				beat == LightningStrikeRules.Beat.CROWN ? 1.9F : 1.25F,
				beat == LightningStrikeRules.Beat.CROWN ? 0.72F : 1.04F);
		PowerFx.sound(level, point, PowersSounds.INTERACTION_CLASH,
				beat == LightningStrikeRules.Beat.CROWN ? 1.05F : 0.68F,
				beat == LightningStrikeRules.Beat.CROWN ? 1.18F : 1.52F);
	}

	/** Draws one main or Communion arc with readable attenuation. */
	public static void chain(ServerLevel level, Vec3 from, Vec3 to,
			int link, boolean soulFork) {
		int color = soulFork ? 0xC8AFFF : link % 2 == 0 ? CYAN : GOLD;
		PowerFx.beam(level, from, to, ParticleTypes.ELECTRIC_SPARK,
				soulFork ? 22 : Math.max(12, 22 - link * 2));
		PowerFx.beam(level, from.add(0.0, 0.12, 0.0), to,
				PowersParticles.RIBBON, soulFork ? 18 : Math.max(10, 17 - link));
		PowerFx.rune(level, to, soulFork ? 0.78 : Math.max(0.46, 0.72 - link * 0.07),
				color, soulFork ? 18 : 14, link * Math.PI / 4.0);
		PowerFx.burst(level, to, soulFork ? PowersParticles.MOTE : ParticleTypes.ELECTRIC_SPARK,
				soulFork ? 16 : 12, 0.42, 0.12);
		PowerFx.sound(level, to, soulFork ? PowersSounds.SOUL_TETHER
				: SoundEvents.TRIDENT_THUNDER.value(), 0.58F,
				soulFork ? 1.48F : 1.35F + link * 0.08F);
	}

	/** Gives every environmental or body counter a semantic double seal and sound. */
	public static void terminal(ServerLevel level, Vec3 point,
			LightningStrikeRules.Counterplay counterplay) {
		if (counterplay == null || LightningStrikeRules.impactAllowed(counterplay)) return;
		int color = switch (counterplay) {
			case UNOWNED, UNLOADED, RESISTED, ROOF, OBSTRUCTED -> 0x8A949C;
			case SAFE_ZONE -> 0x58C7FF;
			case AMETHYST -> 0xB36BFF;
			case SANCTUARY -> 0x8CFF98;
			case KINETIC_WARD, FORCEFIELD -> 0x40C4FF;
			case BODY_ANCHOR -> 0xBCA7FF;
			case TIME_LOCK -> 0xE8FFFF;
			case DARKNESS -> 0x250632;
			case STRIKE, WATER, PURE_LIGHT -> CYAN;
		};
		PowerFx.rune(level, point, 1.18, color, 24,
				level.getGameTime() * 0.19);
		PowerFx.ring(level, point.add(0.0, 0.12, 0.0), 0.72,
				WHITE, 18, -level.getGameTime() * 0.27);
		PowerFx.burst(level, point,
				counterplay == LightningStrikeRules.Counterplay.DARKNESS
						? PowersParticles.ECLIPSE : PowersParticles.FRACTURE,
				18, 0.58, 0.10);
		PowerFx.burst(level, point, ParticleTypes.ELECTRIC_SPARK, 12, 0.48, 0.09);
		PowerFx.sound(level, point, switch (counterplay) {
			case AMETHYST -> PowersSounds.AMETHYST_FRACTURE;
			case DARKNESS -> PowersSounds.DARK_WHISPER;
			case BODY_ANCHOR -> PowersSounds.SOUL_TETHER;
			case TIME_LOCK -> PowersSounds.TIME_SUSPEND;
			case SAFE_ZONE, SANCTUARY, KINETIC_WARD, FORCEFIELD -> PowersSounds.WARD_IMPACT;
			case UNOWNED, UNLOADED, ROOF, OBSTRUCTED, RESISTED -> SoundEvents.BEACON_DEACTIVATE;
			case STRIKE, WATER, PURE_LIGHT -> PowersSounds.INTERACTION_CLASH;
		}, 0.82F, counterplay == LightningStrikeRules.Counterplay.DARKNESS
				? 0.56F : 0.94F);
	}

	/** Marks Motion moving the warned column without hiding its former position. */
	public static void tracking(ServerLevel level, Vec3 from, Vec3 to, int age) {
		if (from.distanceToSqr(to) <= 1.0E-6) return;
		PowerFx.beam(level, from.add(0.0, 0.18, 0.0),
				to.add(0.0, 0.18, 0.0), PowersParticles.RIBBON, 9);
		PowerFx.ring(level, to, 0.62, CYAN, 12, age * 0.38);
	}

	/** Marks Wardcraft grounding a hostile projectile without reflecting ownership. */
	public static void groundedProjectile(ServerLevel level, Vec3 column,
			Vec3 projectile, int index) {
		PowerFx.beam(level, column.add(0.0, 0.4, 0.0), projectile,
				PowersParticles.RIBBON, 8);
		PowerFx.rune(level, projectile, 0.42, CYAN, 12,
				index * Math.PI / 5.0);
		PowerFx.burst(level, projectile, ParticleTypes.ELECTRIC_SPARK,
				8, 0.22, 0.08);
		if (index == 0) PowerFx.sound(level, projectile,
				PowersSounds.WARD_IMPACT, 0.52F, 1.62F);
	}

	/** Shows Insight tearing open a veil only after accepted damage. */
	public static void revelation(ServerLevel level, Vec3 point) {
		PowerFx.rune(level, point, 0.94, CYAN, 22, Math.PI / 4.0);
		PowerFx.ring(level, point.add(0.0, 0.14, 0.0), 0.56,
				GOLD, 16, -Math.PI / 4.0);
		PowerFx.burst(level, point, PowersParticles.GLYPH, 14, 0.48, 0.06);
		PowerFx.burst(level, point, ParticleTypes.END_ROD, 10, 0.38, 0.08);
		PowerFx.sound(level, point, SoundEvents.AMETHYST_BLOCK_CHIME, 0.84F, 1.78F);
	}

	/** Leaves Veil's bounded false storm images after one successful primary verdict. */
	public static void afterimage(ServerLevel level, Vec3 center,
			int memoriesCleared) {
		for (int image = 0; image < 3; image++) {
			double angle = level.getGameTime() * 0.17 + image * Math.PI * 2.0 / 3.0;
			Vec3 point = center.add(Math.cos(angle) * 1.6, 0.2,
					Math.sin(angle) * 1.6);
			PowerFx.rune(level, point, 0.52, STORM_BLUE, 12, -angle);
			PowerFx.spiral(level, point, 0.28, 1.8, CYAN, 10, angle);
		}
		PowerFx.burst(level, center, PowersParticles.RIBBON,
				14 + Math.min(12, memoriesCleared * 2), 1.2, 0.07);
		PowerFx.sound(level, center, PowersSounds.RIFT_CLOSE, 0.58F, 1.34F);
	}

	/** Collapses completion or interruption without leaving harmful weather. */
	public static void close(ServerLevel level, Vec3 center, double radius,
			boolean completed, boolean amethyst, boolean frozen) {
		int color = amethyst ? 0xB36BFF : frozen ? 0xE8FFFF
				: completed ? GOLD : 0x8A949C;
		PowerFx.rune(level, center, Math.min(2.8, radius * 0.78), color,
				completed ? 34 : 24, Math.PI);
		PowerFx.ring(level, center.add(0.0, 0.12, 0.0),
				Math.min(1.9, radius * 0.52), DEEP_CYAN,
				completed ? 28 : 18, -Math.PI);
		PowerFx.burst(level, center.add(0.0, 0.45, 0.0),
				amethyst ? ParticleTypes.ELECTRIC_SPARK : PowersParticles.FRACTURE,
				completed ? 28 : 18, Math.min(1.5, radius * 0.35), 0.11);
		PowerFx.sound(level, center, amethyst ? PowersSounds.AMETHYST_FRACTURE
				: frozen ? PowersSounds.TIME_SUSPEND
						: completed ? PowersSounds.RIFT_CLOSE : SoundEvents.BEACON_DEACTIVATE,
				completed ? 0.86F : 0.66F, completed ? 1.24F : 0.76F);
	}
}

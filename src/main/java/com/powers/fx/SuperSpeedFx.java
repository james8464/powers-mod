package com.powers.fx;

import com.powers.PowersParticles;
import com.powers.PowersSounds;
import com.powers.power.abilities.SuperSpeedRules;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/** Chronal sigils, wakes, and counter-cues dedicated to Chronal Overdrive. */
public final class SuperSpeedFx {
	private static final int CHRONAL = 0x7DEBFF;
	private static final int PALE = 0xD7F8FF;
	private static final int GOLD = 0xFFD166;
	private static final int DEEP = 0x2389A8;

	private SuperSpeedFx() {
	}

	/** Opens the overdrive with nested clock seals that expose every unlocked mastery. */
	public static void open(ServerLevel level, Vec3 center, int durationTicks,
			boolean secondStep, boolean empoweredImpact, boolean afterimage,
			boolean ancientMastery) {
		double outer = ancientMastery ? 1.75 : 1.45;
		PowerFx.rune(level, center.add(0.0, 0.05, 0.0), outer, CHRONAL,
				ancientMastery ? 32 : 24, level.getGameTime() * 0.18);
		PowerFx.ring(level, center.add(0.0, 0.14, 0.0), outer * 0.68,
				empoweredImpact ? GOLD : PALE, empoweredImpact ? 26 : 20,
				-level.getGameTime() * 0.24);
		PowerFx.spiral(level, center.add(0.0, 0.08, 0.0), 0.58,
				afterimage ? 2.8 : 2.1, CHRONAL, afterimage ? 24 : 18, Math.PI / 8.0);
		PowerFx.burst(level, center.add(0.0, 0.9, 0.0), ParticleTypes.ELECTRIC_SPARK,
				ancientMastery ? 26 : 18, 0.62, 0.20);
		PowerFx.burst(level, center.add(0.0, 0.7, 0.0), PowersParticles.GLYPH,
				secondStep ? 14 : 9, 0.46, 0.08);
		PowerFx.sound(level, center, PowersSounds.TIME_SUSPEND, 0.95F,
				ancientMastery ? 1.55F : 1.38F);
		PowerFx.sound(level, center, SoundEvents.FIREWORK_ROCKET_SHOOT, 0.9F,
				1.55F + Math.min(0.25F, durationTicks / 1600.0F));
	}

	/** Draws one measured server wake, switching to a grounded hydroplane language in water. */
	public static void wake(ServerLevel level, Vec3 from, Vec3 to, int segments,
			boolean inWater, int age, boolean afterimage, boolean ancientMastery) {
		if (segments <= 0) return;
		Vec3 start = from.add(0.0, inWater ? 0.22 : 0.48, 0.0);
		Vec3 end = to.add(0.0, inWater ? 0.22 : 0.48, 0.0);
		PowerFx.beam(level, start, end,
				inWater ? ParticleTypes.BUBBLE : PowersParticles.RIBBON, segments);
		PowerFx.beam(level, start.add(0.0, 0.18, 0.0), end.add(0.0, 0.18, 0.0),
				PowersParticles.SPARK, Math.max(2, segments / 2));
		PowerFx.burst(level, end, inWater ? ParticleTypes.SPLASH : ParticleTypes.CLOUD,
				inWater ? 7 : 3, inWater ? 0.38 : 0.18, inWater ? 0.14 : 0.045);
		if (afterimage && age % 4 == 0) {
			PowerFx.ring(level, from.add(0.0, 0.24, 0.0), 0.62, PALE,
					12, age * 0.42);
		}
		if (ancientMastery && age % 6 == 0) {
			PowerFx.burst(level, end.add(0.0, 0.45, 0.0), PowersParticles.GLYPH,
					3, 0.22, 0.025);
		}
	}

	/** Announces water grounding or release so the speed change never feels like a hidden debuff. */
	public static void waterShift(ServerLevel level, Vec3 center, boolean enteredWater) {
		PowerFx.rune(level, center.add(0.0, 0.08, 0.0), enteredWater ? 1.28 : 0.92,
				enteredWater ? DEEP : CHRONAL, enteredWater ? 24 : 18,
				level.getGameTime() * 0.2);
		PowerFx.burst(level, center.add(0.0, 0.45, 0.0),
				enteredWater ? ParticleTypes.SPLASH : ParticleTypes.CLOUD,
				enteredWater ? 18 : 12, 0.58, enteredWater ? 0.18 : 0.09);
		PowerFx.burst(level, center.add(0.0, 0.5, 0.0), PowersParticles.SPARK,
				10, 0.36, 0.08);
		PowerFx.sound(level, center, enteredWater
				? PowersSounds.INTERACTION_CLASH : SoundEvents.BREEZE_SHOOT,
				0.62F, enteredWater ? 0.72F : 1.62F);
	}

	/** Makes a horizontal collision read as a broken time seal rather than ordinary impact. */
	public static void collision(ServerLevel level, Vec3 point) {
		PowerFx.rune(level, point, 0.92, CHRONAL, 18, level.getGameTime() * 0.28);
		PowerFx.burst(level, point, PowersParticles.FRACTURE, 18, 0.52, 0.14);
		PowerFx.burst(level, point, ParticleTypes.ELECTRIC_SPARK, 14, 0.48, 0.18);
		PowerFx.sound(level, point, PowersSounds.INTERACTION_CLASH, 0.8F, 1.62F);
	}

	/** Shows Second Step rewinding the runner out of the collision seal. */
	public static void rebound(ServerLevel level, Vec3 from, Vec3 to) {
		PowerFx.beam(level, from.add(0.0, 0.7, 0.0), to.add(0.0, 0.7, 0.0),
				PowersParticles.RIBBON, 12);
		PowerFx.rune(level, to.add(0.0, 0.08, 0.0), 1.18, PALE, 22, Math.PI);
		PowerFx.ring(level, from.add(0.0, 0.12, 0.0), 0.72, GOLD, 16, 0.0);
		PowerFx.burst(level, to.add(0.0, 0.5, 0.0), ParticleTypes.REVERSE_PORTAL,
				14, 0.42, 0.10);
		PowerFx.sound(level, to, PowersSounds.TIME_RELEASE, 0.75F, 1.72F);
	}

	/** Releases the one-shot, terrain-safe Empowered Impact pressure corona. */
	public static void pressure(ServerLevel level, Vec3 center, int movedTargets) {
		double radius = 2.5 + Math.min(1.5, movedTargets * 0.16);
		PowerFx.rune(level, center.add(0.0, 0.08, 0.0), radius, GOLD,
				32, level.getGameTime() * 0.22);
		PowerFx.ring(level, center.add(0.0, 0.16, 0.0), radius * 0.72,
				PALE, 26, -level.getGameTime() * 0.28);
		PowerFx.burst(level, center.add(0.0, 0.8, 0.0), ParticleTypes.GUST_EMITTER_LARGE,
				3, 1.1, 0.28);
		PowerFx.burst(level, center.add(0.0, 0.8, 0.0), PowersParticles.FRACTURE,
				24, 1.3, 0.17);
		PowerFx.sound(level, center, SoundEvents.WARDEN_SONIC_BOOM, 1.0F, 1.55F);
	}

	/** Gives every refused pressure target a material-specific counter-seal. */
	public static void resistance(ServerLevel level, Vec3 point,
			SuperSpeedRules.PressureDecision decision) {
		if (decision == null || decision == SuperSpeedRules.PressureDecision.MOVE) return;
		int color = switch (decision) {
			case PROTECTED -> 0x58C7FF;
			case AMETHYST -> 0xB36BFF;
			case BODY_ANCHOR -> 0xBCA7FF;
			case FORCEFIELD -> 0x40C4FF;
			case SPELL_WARD -> 0x8CFF98;
			case TIME_LOCK -> 0xE8FFFF;
			case OBSTRUCTED -> 0x8A8F96;
			case MOVE -> CHRONAL;
		};
		PowerFx.ring(level, point, 0.74, color, 16, level.getGameTime() * 0.18);
		PowerFx.ring(level, point.add(0.0, 0.12, 0.0), 0.46, PALE,
				12, -level.getGameTime() * 0.24);
		PowerFx.burst(level, point,
				decision == SuperSpeedRules.PressureDecision.AMETHYST
						? ParticleTypes.ELECTRIC_SPARK : PowersParticles.FRACTURE,
				10, 0.36, 0.075);
		PowerFx.sound(level, point, switch (decision) {
			case AMETHYST -> PowersSounds.AMETHYST_FRACTURE;
			case BODY_ANCHOR -> PowersSounds.SOUL_TETHER;
			case PROTECTED, FORCEFIELD, SPELL_WARD, TIME_LOCK -> PowersSounds.WARD_IMPACT;
			case OBSTRUCTED -> SoundEvents.BEACON_DEACTIVATE;
			case MOVE -> PowersSounds.INTERACTION_CLASH;
		}, 0.58F, decision == SuperSpeedRules.PressureDecision.TIME_LOCK ? 1.65F : 1.08F);
	}

	/** Visualizes one hostile mind losing its freshest image of the runner. */
	public static void memorySlip(ServerLevel level, Vec3 runner, Vec3 observer, int index) {
		Vec3 midpoint = runner.add(observer).scale(0.5).add(0.0, 0.8, 0.0);
		PowerFx.beam(level, observer.add(0.0, 0.8, 0.0), midpoint,
				PowersParticles.MOTE, 6);
		PowerFx.burst(level, observer.add(0.0, 0.8, 0.0), ParticleTypes.REVERSE_PORTAL,
				5, 0.24, 0.045);
		PowerFx.ring(level, runner.add(0.0, 0.28, 0.0), 0.54 + index * 0.035,
				index % 2 == 0 ? PALE : CHRONAL, 10, index * 0.62);
		if (index == 0) PowerFx.sound(level, runner, PowersSounds.TIME_SUSPEND, 0.38F, 1.82F);
	}

	/** Marks Ancient Mastery bending a hostile projectile without reflecting ownership. */
	public static void projectileCurve(ServerLevel level, Vec3 position,
			Vec3 velocity, int index) {
		Vec3 direction = velocity.lengthSqr() > 1.0E-8 ? velocity.normalize() : Vec3.ZERO;
		PowerFx.beam(level, position.subtract(direction.scale(0.9)), position,
				PowersParticles.RIBBON, 7);
		PowerFx.rune(level, position, 0.42, index % 2 == 0 ? GOLD : PALE,
				12, index * 0.54);
		PowerFx.burst(level, position, ParticleTypes.ELECTRIC_SPARK, 4, 0.18, 0.07);
		if (index == 0) PowerFx.sound(level, position, PowersSounds.RUNE_HUM, 0.42F, 1.72F);
	}

	/** Closes a completed or interrupted clock with counter-specific color and sound. */
	public static void finish(ServerLevel level, Vec3 point, boolean interrupted,
			boolean amethyst, boolean frozen, boolean ancientMastery) {
		int color = amethyst ? 0xB36BFF : frozen ? 0xE8FFFF : interrupted ? 0x8A8F96 : CHRONAL;
		PowerFx.rune(level, point.add(0.0, 0.08, 0.0), ancientMastery ? 1.45 : 1.15,
				color, ancientMastery ? 28 : 22, Math.PI);
		PowerFx.burst(level, point.add(0.0, 0.7, 0.0),
				interrupted ? PowersParticles.FRACTURE : PowersParticles.MOTE,
				interrupted ? 22 : 16, 0.58, 0.10);
		PowerFx.sound(level, point,
				amethyst ? PowersSounds.AMETHYST_FRACTURE
						: interrupted ? SoundEvents.BEACON_DEACTIVATE : PowersSounds.TIME_RELEASE,
				0.75F, frozen ? 1.58F : interrupted ? 0.82F : 1.48F);
	}
}

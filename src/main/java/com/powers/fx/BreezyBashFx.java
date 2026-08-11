package com.powers.fx;

import com.powers.PowersParticles;
import com.powers.PowersSounds;
import com.powers.power.abilities.BreezyBashRules;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/** Ancient sky-magic choreography dedicated to the two-stage Tempest Rite. */
public final class BreezyBashFx {
	private static final int SKY = 0x8FE9FF;
	private static final int PALE = 0xD7F8FF;
	private static final int DEEP = 0x4FA8C7;

	private BreezyBashFx() {
	}

	/** Opens the shared eye of the rite with counter-rotating wind seals. */
	public static void open(ServerLevel level, Vec3 center, double radius,
			boolean empowered, boolean ancientMastery) {
		double outer = Math.max(1.4, radius * 0.72);
		PowerFx.rune(level, center.add(0.0, 0.08, 0.0), outer, SKY,
				ancientMastery ? 36 : 28, level.getGameTime() * 0.08);
		PowerFx.ring(level, center.add(0.0, 0.16, 0.0), outer * 0.62, PALE,
				empowered ? 30 : 22, -level.getGameTime() * 0.12);
		PowerFx.spiral(level, center.add(0.0, 0.1, 0.0), outer * 0.36,
				ancientMastery ? 4.6 : 3.4, SKY, ancientMastery ? 32 : 24, 0.0);
		PowerFx.burst(level, center.add(0.0, 0.7, 0.0), ParticleTypes.GUST_EMITTER_LARGE,
				empowered ? 3 : 2, outer * 0.28, 0.16);
		PowerFx.burst(level, center.add(0.0, 0.8, 0.0), PowersParticles.GLYPH,
				ancientMastery ? 18 : 12, outer * 0.22, 0.06);
		PowerFx.sound(level, center, SoundEvents.BREEZE_SHOOT, empowered ? 1.5F : 1.15F, 0.62F);
		PowerFx.sound(level, center, PowersSounds.RUNE_HUM, 0.8F, 1.28F);
	}

	/** Marks one body crossing the owned wind boundary. */
	public static void captured(ServerLevel level, Vec3 center, Vec3 target, int index) {
		PowerFx.beam(level, center.add(0.0, 0.8, 0.0), target,
				index % 2 == 0 ? PowersParticles.RIBBON : PowersParticles.MOTE, 9);
		PowerFx.spiral(level, target.subtract(0.0, 0.7, 0.0), 0.42, 1.8,
				index % 2 == 0 ? SKY : PALE, 14, index * 0.65);
		PowerFx.ring(level, target.subtract(0.0, 0.72, 0.0), 0.58,
				SKY, 12, level.getGameTime() * 0.14);
		PowerFx.burst(level, target, ParticleTypes.GUST, 5, 0.28, 0.10);
	}

	/** Sustains a sparse rising eye while captured bodies approach the apex. */
	public static void sustain(ServerLevel level, Vec3 center, double radius,
			int age, boolean ancientMastery) {
		double phase = age * 0.16;
		double outer = Math.max(1.4, radius * 0.50);
		PowerFx.ring(level, center.add(0.0, 0.12, 0.0), outer, SKY,
				ancientMastery ? 26 : 18, phase);
		PowerFx.ring(level, center.add(0.0, 1.8, 0.0), outer * 0.56, PALE,
				ancientMastery ? 20 : 14, -phase * 1.2);
		PowerFx.burst(level, center.add(0.0, 1.0, 0.0), com.powers.PowersParticles.RIBBON,
				ancientMastery ? 5 : 3, outer * 0.30, 0.035);
		if (age % 6 == 0) PowerFx.sound(level, center, PowersSounds.RUNE_HUM, 0.26F, 1.52F);
	}

	/** Draws a short ownership tether without writing movement. */
	public static void tether(ServerLevel level, Vec3 center, Vec3 target, int index) {
		PowerFx.beam(level, center.add(0.0, 1.0, 0.0), target,
				index % 2 == 0 ? PowersParticles.MOTE : PowersParticles.RIBBON, 6);
		PowerFx.coloredBurst(level, target, index % 2 == 0 ? SKY : PALE, 2, 0.14);
	}

	/** Contracts one captured body's helix into the downward verdict. */
	public static void slam(ServerLevel level, Vec3 point, boolean empowered, int index) {
		PowerFx.ring(level, point, empowered ? 0.92 : 0.72, PALE,
				empowered ? 22 : 16, -level.getGameTime() * 0.22);
		PowerFx.spiral(level, point.add(0.0, 1.3, 0.0), 0.34,
				-1.7, index % 2 == 0 ? SKY : DEEP, 16, index * 0.45);
		PowerFx.beam(level, point.add(0.0, 1.8, 0.0), point.subtract(0.0, 0.7, 0.0),
				PowersParticles.FRACTURE, 10);
		PowerFx.burst(level, point, ParticleTypes.GUST_EMITTER_LARGE,
				empowered ? 2 : 1, 0.48, 0.18);
		PowerFx.sound(level, point, SoundEvents.BREEZE_SHOOT, empowered ? 0.9F : 0.62F,
				0.72F + Math.min(0.3F, index * 0.025F));
	}

	/** Closes Empowered Impact as one terrain-safe pressure corona. */
	public static void pressure(ServerLevel level, Vec3 center, double radius) {
		double visualRadius = Math.max(1.8, Math.min(6.0, radius * 0.58));
		PowerFx.rune(level, center.add(0.0, 0.08, 0.0), visualRadius, PALE,
				38, level.getGameTime() * 0.16);
		PowerFx.burst(level, center.add(0.0, 0.8, 0.0), ParticleTypes.GUST_EMITTER_LARGE,
				4, visualRadius * 0.38, 0.28);
		PowerFx.burst(level, center.add(0.0, 0.8, 0.0), PowersParticles.FRACTURE,
				30, visualRadius * 0.42, 0.16);
		PowerFx.sound(level, center, SoundEvents.WARDEN_SONIC_BOOM, 1.05F, 1.42F);
	}

	/** Shows Ancient Mastery bending a projectile without claiming its owner. */
	public static void projectileCurve(ServerLevel level, Vec3 position, Vec3 velocity, int index) {
		Vec3 direction = velocity.lengthSqr() > 1.0E-8 ? velocity.normalize() : Vec3.ZERO;
		PowerFx.beam(level, position.subtract(direction.scale(0.8)), position,
				PowersParticles.RIBBON, 6);
		PowerFx.ring(level, position, 0.38, index % 2 == 0 ? SKY : PALE,
				10, index * 0.5);
		PowerFx.burst(level, position, ParticleTypes.GUST, 3, 0.16, 0.06);
	}

	/** Gives every rejected capture a material-specific, rate-bounded response. */
	public static void resistance(ServerLevel level, Vec3 point,
			BreezyBashRules.CaptureDecision decision) {
		if (decision == null || decision == BreezyBashRules.CaptureDecision.CAPTURE) return;
		int primary = switch (decision) {
			case PROTECTED -> 0x58C7FF;
			case AMETHYST -> 0xB36BFF;
			case BODY_ANCHOR -> 0xBCA7FF;
			case FORCEFIELD -> 0x40C4FF;
			case SPELL_WARD -> 0x8CFF98;
			case TIME_LOCK -> 0xE8FFFF;
			case CEILING -> 0x8A8F96;
			case WIND_RESONANCE -> 0x4FD1C5;
			case CAPTURE -> SKY;
		};
		PowerFx.ring(level, point, 0.78, primary, 16, level.getGameTime() * 0.16);
		PowerFx.ring(level, point.add(0.0, 0.12, 0.0), 0.48, PALE,
				12, -level.getGameTime() * 0.2);
		PowerFx.burst(level, point,
				decision == BreezyBashRules.CaptureDecision.AMETHYST
						? ParticleTypes.ELECTRIC_SPARK : PowersParticles.FRACTURE,
				10, 0.38, 0.07);
		PowerFx.sound(level, point, switch (decision) {
			case AMETHYST -> PowersSounds.AMETHYST_FRACTURE;
			case BODY_ANCHOR -> PowersSounds.SOUL_TETHER;
			case WIND_RESONANCE -> PowersSounds.INTERACTION_CLASH;
			case CEILING -> SoundEvents.BEACON_DEACTIVATE;
			case PROTECTED, FORCEFIELD, SPELL_WARD, TIME_LOCK -> PowersSounds.WARD_IMPACT;
			case CAPTURE -> SoundEvents.BREEZE_SHOOT;
		}, 0.58F, decision == BreezyBashRules.CaptureDecision.TIME_LOCK ? 1.55F : 1.05F);
	}

	/** Releases a body safely beneath a pale feather-like corona. */
	public static void released(ServerLevel level, Vec3 point) {
		PowerFx.ring(level, point, 0.64, PALE, 14, level.getGameTime() * 0.13);
		PowerFx.burst(level, point, com.powers.PowersParticles.RIBBON, 9, 0.34, 0.07);
		PowerFx.burst(level, point, PowersParticles.MOTE, 7, 0.28, 0.04);
	}

	/** Breaks the central seal for either an interrupted or completed rite. */
	public static void close(ServerLevel level, Vec3 center, double radius, boolean interrupted) {
		double outer = Math.max(1.4, Math.min(7.0, radius * 0.72));
		PowerFx.ring(level, center.add(0.0, 0.08, 0.0), outer,
				interrupted ? 0x8A8F96 : SKY, 30, level.getGameTime() * 0.16);
		PowerFx.burst(level, center.add(0.0, 0.7, 0.0),
				interrupted ? PowersParticles.FRACTURE : ParticleTypes.GUST,
				interrupted ? 24 : 16, outer * 0.30, 0.12);
		PowerFx.sound(level, center,
				interrupted ? SoundEvents.BEACON_DEACTIVATE : PowersSounds.RIFT_CLOSE,
				0.72F, interrupted ? 0.78F : 1.42F);
	}

	/** Collapses an empty offer without committing energy or cooldown. */
	public static void empty(ServerLevel level, Vec3 center) {
		PowerFx.ring(level, center.add(0.0, 0.08, 0.0), 0.72, DEEP,
				14, Math.PI);
		PowerFx.burst(level, center.add(0.0, 0.5, 0.0), com.powers.PowersParticles.RIBBON,
				10, 0.42, 0.05);
		PowerFx.sound(level, center, SoundEvents.BEACON_DEACTIVATE, 0.48F, 1.25F);
	}
}

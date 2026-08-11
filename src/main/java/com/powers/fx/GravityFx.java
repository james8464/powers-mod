package com.powers.fx;

import com.powers.PowersParticles;
import com.powers.PowersSounds;
import com.powers.power.abilities.GravityDisplacementRules;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/** Ancient-magic choreography dedicated to the Gravity Displacement orrery. */
public final class GravityFx {
	private GravityFx() {
	}

	/** Opens the counter-rotating gravitational orrery around its fixed world anchor. */
	public static void open(ServerLevel level, Vec3 center, double radius, boolean ancientMastery) {
		double outer = Math.max(1.5, radius * 0.78);
		PowerFx.rune(level, center.add(0.0, 0.08, 0.0), outer, 0x8C66FF,
				ancientMastery ? 40 : 32, 0.0);
		PowerFx.ring(level, center.add(0.0, 1.45, 0.0), outer * 0.62, 0xB9E7FF,
				ancientMastery ? 32 : 24, Math.PI / 8.0);
		PowerFx.ring(level, center.add(0.0, 2.7, 0.0), outer * 0.36, 0xD9C7FF,
				ancientMastery ? 24 : 18, -Math.PI / 8.0);
		PowerFx.spiral(level, center.add(0.0, 0.05, 0.0), outer * 0.48,
				ancientMastery ? 4.4 : 3.4, 0x8C66FF, ancientMastery ? 34 : 26, 0.0);
		PowerFx.burst(level, center.add(0.0, 1.0, 0.0), com.powers.PowersParticles.ECLIPSE,
				ancientMastery ? 30 : 22, outer * 0.32, 0.08);
		PowerFx.burst(level, center.add(0.0, 0.6, 0.0), PowersParticles.GLYPH,
				ancientMastery ? 18 : 12, outer * 0.25, 0.035);
		PowerFx.sound(level, center, PowersSounds.RUNE_HUM, ancientMastery ? 1.4F : 1.0F, 0.52F);
		PowerFx.sound(level, center, SoundEvents.END_PORTAL_SPAWN,
				ancientMastery ? 1.2F : 0.85F, 0.72F);
	}

	/** Sustains the field with sparse, height-readable rings under the global particle budget. */
	public static void sustain(ServerLevel level, Vec3 center, double radius,
			int age, boolean ancientMastery) {
		double phase = age * 0.07;
		double outer = Math.max(1.5, radius * 0.78);
		PowerFx.ring(level, center.add(0.0, 0.08, 0.0), outer, 0x8C66FF,
				ancientMastery ? 34 : 26, phase);
		PowerFx.ring(level, center.add(0.0, 1.55, 0.0), outer * 0.58, 0xB9E7FF,
				ancientMastery ? 28 : 20, -phase * 1.25);
		PowerFx.ring(level, center.add(0.0, 2.9, 0.0), outer * 0.32, 0xD9C7FF,
				ancientMastery ? 22 : 16, phase * 1.6);
		PowerFx.burst(level, center.add(0.0, 1.2, 0.0), com.powers.PowersParticles.ECLIPSE,
				ancientMastery ? 6 : 4, outer * 0.22, 0.025);
		if (age % 20 == 0) {
			PowerFx.spiral(level, center.add(0.0, 0.1, 0.0), outer * 0.34, 3.2,
					0x8C66FF, ancientMastery ? 22 : 16, phase);
			PowerFx.sound(level, center, PowersSounds.RUNE_HUM, 0.38F, 0.48F);
		}
	}

	/** Marks the instant a body crosses the orrery boundary. */
	public static void captured(ServerLevel level, Vec3 center, Vec3 target,
			boolean ancientMastery) {
		PowerFx.beam(level, center.add(0.0, 1.0, 0.0), target, PowersParticles.RIBBON,
				ancientMastery ? 14 : 10);
		PowerFx.burst(level, target, PowersParticles.GLYPH, ancientMastery ? 10 : 7, 0.35, 0.06);
		PowerFx.burst(level, target, com.powers.PowersParticles.ECLIPSE,
				ancientMastery ? 12 : 8, 0.42, 0.08);
		PowerFx.ring(level, target, 0.62, 0x8C66FF, ancientMastery ? 16 : 12, 0.0);
	}

	/** Draws one short gravitational tether without revealing hidden entities. */
	public static void tether(ServerLevel level, Vec3 center, Vec3 target,
			boolean ancientMastery, int index) {
		PowerFx.beam(level, center, target,
				index % 2 == 0 ? PowersParticles.RIBBON : PowersParticles.MOTE,
				ancientMastery ? 9 : 6);
		PowerFx.coloredBurst(level, target, index % 2 == 0 ? 0x8C66FF : 0xB9E7FF,
				ancientMastery ? 3 : 2, 0.18);
	}

	/** Gives each gravity counter a distinct, rate-limited magical ceremony. */
	public static void resistance(ServerLevel level, Vec3 point,
			GravityDisplacementRules.CaptureDecision decision) {
		if (decision == null || decision == GravityDisplacementRules.CaptureDecision.CAPTURE) return;
		int primary = switch (decision) {
			case PROTECTED -> 0x58C7FF;
			case AMETHYST -> 0xB36BFF;
			case BODY_ANCHOR -> 0xBCA7FF;
			case FORCEFIELD -> 0x40C4FF;
			case SPELL_WARD -> 0x8CFF98;
			case TIME_LOCK -> 0x96F5FF;
			case GRAVITY_RESONANCE -> 0xD66BFF;
			case CAPTURE -> 0x8C66FF;
		};
		int secondary = switch (decision) {
			case PROTECTED -> 0xD6F5FF;
			case AMETHYST -> 0x5E2A84;
			case BODY_ANCHOR -> 0x8FE9FF;
			case FORCEFIELD -> 0xE8F8FF;
			case SPELL_WARD -> 0xFFE8A3;
			case TIME_LOCK -> 0xFFFFFF;
			case GRAVITY_RESONANCE, CAPTURE -> 0xB9E7FF;
		};
		PowerFx.ring(level, point, 0.85, primary, 16, level.getGameTime() * 0.12);
		PowerFx.ring(level, point.add(0.0, 0.12, 0.0), 0.56, secondary, 12,
				-level.getGameTime() * 0.18);
		PowerFx.burst(level, point,
				decision == GravityDisplacementRules.CaptureDecision.AMETHYST
						? ParticleTypes.ELECTRIC_SPARK : PowersParticles.FRACTURE,
				10, 0.42, 0.07);
		PowerFx.sound(level, point,
				decision == GravityDisplacementRules.CaptureDecision.AMETHYST
						? PowersSounds.AMETHYST_FRACTURE
						: decision == GravityDisplacementRules.CaptureDecision.BODY_ANCHOR
								? PowersSounds.SOUL_TETHER
						: decision == GravityDisplacementRules.CaptureDecision.GRAVITY_RESONANCE
								? PowersSounds.INTERACTION_CLASH : PowersSounds.WARD_IMPACT,
				0.62F, decision == GravityDisplacementRules.CaptureDecision.TIME_LOCK ? 1.42F : 0.86F);
	}

	/** Resolves two overlapping gravity fields as one readable resonance handoff. */
	public static void resonance(ServerLevel level, Vec3 first, Vec3 second, Vec3 target) {
		PowerFx.beam(level, first.add(0.0, 1.0, 0.0), target, PowersParticles.RIBBON, 8);
		PowerFx.beam(level, second.add(0.0, 1.0, 0.0), target, PowersParticles.MOTE, 8);
		PowerFx.ring(level, target, 0.9, 0xD66BFF, 18, level.getGameTime() * 0.14);
		PowerFx.ring(level, target.add(0.0, 0.12, 0.0), 0.62, 0xB9E7FF, 14,
				-level.getGameTime() * 0.18);
		PowerFx.burst(level, target, PowersParticles.FRACTURE, 12, 0.46, 0.09);
		PowerFx.sound(level, target, PowersSounds.INTERACTION_CLASH, 0.72F, 0.78F);
	}

	/** Shows a hostile projectile being curved, never reflected or reassigned. */
	public static void projectileCurve(ServerLevel level, Vec3 position, Vec3 velocity, long age) {
		Vec3 tail = position.subtract(velocity.normalize().scale(0.9));
		PowerFx.beam(level, tail, position, PowersParticles.RIBBON, 6);
		PowerFx.burst(level, position, com.powers.PowersParticles.ECLIPSE, 3, 0.14, 0.035);
		PowerFx.coloredBurst(level, position, 0xB9E7FF, 2, 0.12);
		if (age % 10 == 0) PowerFx.sound(level, position, PowersSounds.WARD_IMPACT, 0.28F, 1.6F);
	}

	/** Contracts the orrery into either a safe release or an empowered impact. */
	public static void collapse(ServerLevel level, Vec3 center, double radius,
			boolean empowered, boolean interrupted, boolean ancientMastery) {
		double outer = Math.max(1.5, Math.min(8.0, radius * 0.78));
		PowerFx.ring(level, center.add(0.0, 0.1, 0.0), outer,
				interrupted ? 0xB36BFF : 0x8C66FF, ancientMastery ? 42 : 34, Math.PI / 8.0);
		PowerFx.ring(level, center.add(0.0, 1.5, 0.0), outer * 0.58, 0xB9E7FF,
				ancientMastery ? 34 : 26, -Math.PI / 8.0);
		PowerFx.burst(level, center.add(0.0, 1.0, 0.0),
				ColorParticleOption.create(ParticleTypes.FLASH, 0xFFD9C7FF),
				empowered ? 5 : 2, 0.45, 0.0);
		PowerFx.burst(level, center.add(0.0, 1.0, 0.0), PowersParticles.FRACTURE,
				empowered ? 36 : 22, empowered ? 2.0 : 1.2, empowered ? 0.22 : 0.10);
		PowerFx.burst(level, center.add(0.0, 1.0, 0.0), com.powers.PowersParticles.ECLIPSE,
				ancientMastery ? 30 : 22, outer * 0.34, 0.16);
		PowerFx.sound(level, center,
				interrupted ? PowersSounds.AMETHYST_FRACTURE : PowersSounds.RIFT_CLOSE,
				empowered ? 1.35F : 0.9F, interrupted ? 0.82F : 0.52F);
		if (empowered) {
			PowerFx.sound(level, center, SoundEvents.WARDEN_SONIC_BOOM, 1.5F, 0.62F);
			PowerFx.sound(level, center, SoundEvents.GENERIC_EXPLODE.value(), 1.25F, 0.72F);
		}
	}
}

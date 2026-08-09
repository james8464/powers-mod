package com.powers.fx;

import com.powers.PowersParticles;
import com.powers.PowersSounds;
import com.powers.power.abilities.GroundSlamRules;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/** Ochre-violet earth choreography dedicated to Faultbound Verdict. */
public final class GroundSlamFx {
	private static final int OCHRE = 0xC98A3A;
	private static final int PALE_EARTH = 0xE6C98B;
	private static final int VIOLET = 0x7A4FB3;
	private static final int DEEP_EARTH = 0x4B3426;

	private GroundSlamFx() {
	}

	/** Opens the complete fault clock before any damaging beat. */
	public static void open(ServerLevel level, Vec3 center, double radius,
			boolean moving, boolean soulEcho, boolean ancientMastery) {
		double outer = Math.max(2.0, radius * 0.82);
		PowerFx.rune(level, center.add(0.0, 0.08, 0.0), outer, OCHRE,
				ancientMastery ? 42 : 32, level.getGameTime() * 0.07);
		PowerFx.ring(level, center.add(0.0, 0.14, 0.0), outer * 0.68,
				moving ? 0x8FE9FF : VIOLET, soulEcho ? 30 : 24,
				-level.getGameTime() * 0.11);
		PowerFx.spiral(level, center.add(0.0, 0.2, 0.0), outer * 0.30,
				2.8, PALE_EARTH, ancientMastery ? 30 : 22, 0.0);
		PowerFx.burst(level, center.add(0.0, 0.45, 0.0), PowersParticles.GLYPH,
				ancientMastery ? 18 : 12, outer * 0.24, 0.05);
		PowerFx.sound(level, center, PowersSounds.RUNE_HUM, 0.82F, 0.62F);
		PowerFx.sound(level, center, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.68F, 0.74F);
	}

	/** Sustains contracting fault spokes during the twelve-tick warning. */
	public static void omen(ServerLevel level, Vec3 center, double radius,
			int age, boolean afterimage, boolean ancientMastery) {
		double progress = Math.max(0.0, Math.min(1.0, age / 12.0));
		double phase = age * 0.20;
		PowerFx.ring(level, center.add(0.0, 0.07, 0.0),
				radius * (0.92 - progress * 0.34), OCHRE, 26, phase);
		PowerFx.rune(level, center.add(0.0, 0.12, 0.0),
				1.0 + progress * 1.35, ancientMastery ? PALE_EARTH : VIOLET,
				18 + (int) (progress * 10), -phase * 1.3);
		PowerFx.burst(level, center.add(0.0, 0.5, 0.0),
				afterimage ? PowersParticles.RIBBON : PowersParticles.FRACTURE,
				3 + (int) (progress * 4), radius * 0.20, 0.035);
		if (age == 6) PowerFx.sound(level, center, PowersSounds.RUNE_HUM, 0.48F, 0.78F);
		if (age == 10) PowerFx.sound(level, center, SoundEvents.ANVIL_LAND, 0.72F, 0.64F);
	}

	/** Shows the exact next epicentre during its final three counterplay ticks. */
	public static void telegraph(ServerLevel level, Vec3 point,
			int ticksUntil, GroundSlamRules.Beat beat) {
		double progress = 1.0 - Math.max(0, ticksUntil - 1) / 3.0;
		double radius = beat == GroundSlamRules.Beat.CROWN
				? 2.2 + progress * 1.6 : 0.8 + progress * 0.9;
		int color = beat == GroundSlamRules.Beat.SOUL_ECHO ? 0xBCA7FF
				: beat == GroundSlamRules.Beat.CROWN ? PALE_EARTH : OCHRE;
		PowerFx.rune(level, point, radius, color,
				beat == GroundSlamRules.Beat.CROWN ? 36 : 22,
				level.getGameTime() * 0.22);
		PowerFx.ring(level, point.add(0.0, 0.08, 0.0), radius * 0.58,
				VIOLET, beat == GroundSlamRules.Beat.CROWN ? 28 : 16,
				-level.getGameTime() * 0.28);
		PowerFx.burst(level, point.add(0.0, 0.18, 0.0), PowersParticles.FRACTURE,
				beat == GroundSlamRules.Beat.CROWN ? 9 : 4, radius * 0.28, 0.025);
		if (ticksUntil == 1) {
			PowerFx.sound(level, point, SoundEvents.RESPAWN_ANCHOR_CHARGE,
					beat == GroundSlamRules.Beat.CROWN ? 1.0F : 0.58F,
					beat == GroundSlamRules.Beat.CROWN ? 0.58F : 0.82F);
		}
	}

	/** Releases one radial seismic beat with rank and medium-specific layers. */
	public static void impact(ServerLevel level, Vec3 point, double radius,
			GroundSlamRules.Beat beat, GroundSlamRules.Counterplay medium,
			int affected, boolean empoweredImpact) {
		int primary = switch (medium) {
			case WATER -> 0x69D5FF;
			case DARKNESS -> 0x2A0C3D;
			case PURE_LIGHT -> 0xFFF6C7;
			default -> beat == GroundSlamRules.Beat.SOUL_ECHO ? 0xBCA7FF
					: beat == GroundSlamRules.Beat.CROWN ? PALE_EARTH : OCHRE;
		};
		int secondary = medium == GroundSlamRules.Counterplay.DARKNESS
				? VIOLET : empoweredImpact ? 0xFFF0C2 : DEEP_EARTH;
		PowerFx.rune(level, point, radius, primary,
				beat == GroundSlamRules.Beat.CROWN ? 52 : 36,
				level.getGameTime() * 0.18);
		PowerFx.ring(level, point.add(0.0, 0.10, 0.0), radius * 0.72,
				secondary, beat == GroundSlamRules.Beat.CROWN ? 42 : 30,
				-level.getGameTime() * 0.24);
		PowerFx.ring(level, point.add(0.0, 0.20, 0.0), radius * 0.38,
				primary, beat == GroundSlamRules.Beat.CROWN ? 34 : 22,
				level.getGameTime() * 0.34);
		PowerFx.burst(level, point.add(0.0, 0.45, 0.0), PowersParticles.FRACTURE,
				26 + Math.min(24, affected * 3), radius * 0.44, 0.20);
		PowerFx.burst(level, point.add(0.0, 0.6, 0.0),
				medium == GroundSlamRules.Counterplay.WATER
						? ParticleTypes.SPLASH : ParticleTypes.CAMPFIRE_COSY_SMOKE,
				18 + Math.min(16, affected * 2), radius * 0.34, 0.13);
		if (medium == GroundSlamRules.Counterplay.PURE_LIGHT) {
			PowerFx.burst(level, point.add(0.0, 0.7, 0.0), ParticleTypes.END_ROD,
					24, radius * 0.32, 0.15);
		}
		PowerFx.sound(level, point, SoundEvents.GENERIC_EXPLODE.value(),
				beat == GroundSlamRules.Beat.CROWN ? 1.75F : 1.12F,
				beat == GroundSlamRules.Beat.SOUL_ECHO ? 1.18F
						: beat == GroundSlamRules.Beat.CROWN ? 0.52F : 0.72F);
		PowerFx.sound(level, point, PowersSounds.INTERACTION_CLASH, 0.68F,
				medium == GroundSlamRules.Counterplay.DARKNESS ? 0.62F : 0.94F);
	}

	/** Gives each refused environment or body interaction a semantic double seal. */
	public static void counter(ServerLevel level, Vec3 point,
			GroundSlamRules.Counterplay counterplay) {
		if (counterplay == null || GroundSlamRules.impactAllowed(counterplay)) return;
		int color = switch (counterplay) {
			case UNLOADED, UNSUPPORTED, COLLISION, RESISTED -> 0x8A8F96;
			case SAFE_ZONE, PROTECTED -> 0x58C7FF;
			case AMETHYST -> 0xB36BFF;
			case SANCTUARY -> 0x8CFF98;
			case KINETIC_WARD, FORCEFIELD -> 0x40C4FF;
			case BODY_ANCHOR -> 0xBCA7FF;
			case TIME_LOCK -> 0xE8FFFF;
			case IMPACT, WATER, DARKNESS, PURE_LIGHT -> OCHRE;
		};
		PowerFx.rune(level, point, 1.08, color, 22,
				level.getGameTime() * 0.16);
		PowerFx.ring(level, point.add(0.0, 0.11, 0.0), 0.66,
				PALE_EARTH, 16, -level.getGameTime() * 0.22);
		PowerFx.burst(level, point, counterplay == GroundSlamRules.Counterplay.AMETHYST
				? ParticleTypes.ELECTRIC_SPARK : PowersParticles.FRACTURE,
				14, 0.46, 0.09);
		PowerFx.sound(level, point, switch (counterplay) {
			case AMETHYST -> PowersSounds.AMETHYST_FRACTURE;
			case BODY_ANCHOR -> PowersSounds.SOUL_TETHER;
			case TIME_LOCK -> PowersSounds.TIME_SUSPEND;
			case SAFE_ZONE, PROTECTED, SANCTUARY, KINETIC_WARD, FORCEFIELD ->
					PowersSounds.WARD_IMPACT;
			case UNLOADED, UNSUPPORTED, COLLISION, RESISTED -> SoundEvents.BEACON_DEACTIVATE;
			case IMPACT, WATER, DARKNESS, PURE_LIGHT -> PowersSounds.INTERACTION_CLASH;
		}, 0.72F, counterplay == GroundSlamRules.Counterplay.TIME_LOCK ? 1.48F : 0.86F);
	}

	/** Shows one successfully moved body riding the outgoing fault edge. */
	public static void pressure(ServerLevel level, Vec3 center, Vec3 target, int index) {
		PowerFx.beam(level, center.add(0.0, 0.2, 0.0), target,
				PowersParticles.FRACTURE, 7);
		PowerFx.ring(level, target, 0.48, index % 2 == 0 ? OCHRE : VIOLET,
				10, index * 0.42);
		PowerFx.burst(level, target, ParticleTypes.CAMPFIRE_COSY_SMOKE,
				4, 0.24, 0.05);
	}

	/** Marks Insight naming a veil torn by a successful seismic hit. */
	public static void revelation(ServerLevel level, Vec3 point) {
		PowerFx.rune(level, point, 0.86, 0x8FE9FF, 18, Math.PI / 4.0);
		PowerFx.burst(level, point, PowersParticles.GLYPH, 12, 0.42, 0.06);
		PowerFx.sound(level, point, SoundEvents.AMETHYST_BLOCK_CHIME, 0.72F, 1.62F);
	}

	/** Shows Motion carrying the still-warning fault clock. */
	public static void tracking(ServerLevel level, Vec3 from, Vec3 to, int age) {
		if (from.distanceToSqr(to) <= 1.0E-6) return;
		PowerFx.beam(level, from.add(0.0, 0.12, 0.0), to.add(0.0, 0.12, 0.0),
				PowersParticles.RIBBON, 8);
		PowerFx.ring(level, to, 0.58, 0x8FE9FF, 12, age * 0.24);
	}

	/** Authors Wardcraft's stone mantle and Veil's dust shroud on the caster. */
	public static void mantle(ServerLevel level, Vec3 point,
			boolean wardcraft, boolean afterimage) {
		if (!wardcraft && !afterimage) return;
		if (wardcraft) {
			PowerFx.rune(level, point, 1.18, PALE_EARTH, 24,
					level.getGameTime() * 0.15);
			PowerFx.ring(level, point.add(0.0, 0.95, 0.0), 0.82,
					OCHRE, 20, -level.getGameTime() * 0.18);
		}
		if (afterimage) {
			PowerFx.spiral(level, point.add(0.0, 0.2, 0.0), 0.68,
					2.2, VIOLET, 22, Math.PI / 3.0);
			PowerFx.burst(level, point.add(0.0, 0.8, 0.0),
					ParticleTypes.CAMPFIRE_COSY_SMOKE, 18, 0.55, 0.08);
		}
		PowerFx.sound(level, point, PowersSounds.RUNE_HUM, 0.62F,
				wardcraft && afterimage ? 0.72F : 1.08F);
	}

	/** Marks one policy-approved soft block removed without item drops. */
	public static void terrainFracture(ServerLevel level, Vec3 point, int index) {
		PowerFx.rune(level, point, 0.34 + index * 0.012, DEEP_EARTH,
				8, index * GOLDEN_PHASE);
		PowerFx.burst(level, point, PowersParticles.FRACTURE, 5, 0.22, 0.06);
	}

	/** Collapses a completed or interrupted fault clock without damaging residue. */
	public static void close(ServerLevel level, Vec3 center, double radius,
			boolean completed, boolean amethyst, boolean frozen) {
		int color = amethyst ? 0xB36BFF : frozen ? 0xE8FFFF
				: completed ? OCHRE : 0x8A8F96;
		PowerFx.rune(level, center, Math.min(3.6, radius * 0.62), color,
				completed ? 34 : 24, Math.PI);
		PowerFx.ring(level, center.add(0.0, 0.10, 0.0),
				Math.min(2.4, radius * 0.42), VIOLET,
				completed ? 28 : 18, -Math.PI);
		PowerFx.burst(level, center.add(0.0, 0.4, 0.0),
				amethyst ? ParticleTypes.ELECTRIC_SPARK : PowersParticles.FRACTURE,
				completed ? 26 : 18, Math.min(1.5, radius * 0.28), 0.10);
		PowerFx.sound(level, center, amethyst ? PowersSounds.AMETHYST_FRACTURE
				: frozen ? PowersSounds.TIME_SUSPEND
						: completed ? PowersSounds.RIFT_CLOSE : SoundEvents.BEACON_DEACTIVATE,
				completed ? 0.82F : 0.62F, completed ? 0.86F : 0.74F);
	}

	private static final double GOLDEN_PHASE = Math.PI * (3.0 - Math.sqrt(5.0));
}

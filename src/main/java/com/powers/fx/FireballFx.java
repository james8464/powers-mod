package com.powers.fx;

import com.powers.PowersParticles;
import com.powers.PowersSounds;
import com.powers.power.abilities.FireballRules;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/** Ancient flame choreography dedicated to the chargeable Cinderheart. */
public final class FireballFx {
	private static final int EMBER = 0xFF5A24;
	private static final int GOLD = 0xFFD166;
	private static final int WHITE = 0xFFF4D6;
	private static final int COAL = 0x4A1710;
	private static final int STEAM = 0xD7F8FF;

	private FireballFx() {
	}

	/** Opens a hovering heart beneath layered flame seals. */
	public static void open(ServerLevel level, Vec3 point, int tier,
			boolean empoweredImpact, boolean ancientMastery) {
		double outer = 0.82 + tier * 0.20 + (ancientMastery ? 0.18 : 0.0);
		PowerFx.rune(level, point, outer, EMBER, 18 + tier * 4,
				level.getGameTime() * 0.12);
		PowerFx.ring(level, point.add(0.0, 0.12, 0.0), outer * 0.68,
				empoweredImpact ? GOLD : WHITE, 14 + tier * 3,
				-level.getGameTime() * 0.18);
		PowerFx.spiral(level, point.subtract(0.0, 0.55, 0.0), 0.32 + tier * 0.05,
				1.1 + tier * 0.25, EMBER, 14 + tier * 3, 0.0);
		PowerFx.clarityBurst(level, point, ParticleTypes.FLAME, 14 + tier * 4, 0.34, 0.09);
		PowerFx.clarityBurst(level, point, PowersParticles.GLYPH, 6 + tier * 2, 0.28, 0.035);
		PowerFx.sound(level, point, SoundEvents.FIRECHARGE_USE, 0.82F + tier * 0.08F,
				1.18F - tier * 0.07F);
		PowerFx.sound(level, point, PowersSounds.RUNE_HUM, 0.45F, 0.82F + tier * 0.08F);
	}

	/** Deepens an existing heart by one paid seal without spawning another entity. */
	public static void charge(ServerLevel level, Vec3 point, int tier,
			boolean empoweredImpact, boolean ancientMastery) {
		double outer = 0.92 + tier * 0.28;
		PowerFx.rune(level, point, outer, tier >= 4 ? WHITE : EMBER,
				22 + tier * 5, level.getGameTime() * 0.2);
		PowerFx.rune(level, point.add(0.0, 0.12, 0.0), outer * 0.67,
				empoweredImpact ? GOLD : WHITE, 18 + tier * 4,
				-level.getGameTime() * 0.27);
		PowerFx.spiral(level, point.subtract(0.0, 0.75, 0.0), 0.42,
				1.5 + tier * 0.34, tier >= 4 ? GOLD : EMBER, 20 + tier * 4, tier);
		PowerFx.clarityBurst(level, point, ParticleTypes.FLAME, 18 + tier * 7, 0.48, 0.16);
		PowerFx.clarityBurst(level, point, ParticleTypes.ELECTRIC_SPARK,
				ancientMastery ? 18 : 10, 0.38, 0.10);
		PowerFx.sound(level, point, PowersSounds.RANK_AWAKEN, 0.75F + tier * 0.08F,
				0.76F + tier * 0.12F);
		PowerFx.sound(level, point, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.72F,
				0.88F + tier * 0.08F);
	}

	/** Refuses an extra charge after launch or after the rank's highest seal. */
	public static void chargeDenied(ServerLevel level, Vec3 point, int tier,
			boolean launched) {
		PowerFx.rune(level, point, 0.72 + tier * 0.10, COAL,
				16 + tier * 2, Math.PI);
		PowerFx.ring(level, point.add(0.0, 0.10, 0.0), 0.48 + tier * 0.07,
				launched ? 0x8A8F96 : EMBER, 12 + tier * 2, -Math.PI / 4.0);
		PowerFx.clarityBurst(level, point, PowersParticles.FRACTURE, 10 + tier * 2, 0.34, 0.06);
		PowerFx.sound(level, point, SoundEvents.BEACON_DEACTIVATE, 0.52F,
				launched ? 1.18F : 0.72F);
	}

	/** Sustains a sparse heartbeat so an armed orb remains readable without particle spam. */
	public static void hover(ServerLevel level, Vec3 point, int tier, int age,
			boolean afterimage, boolean trueSight, boolean ancientMastery) {
		double phase = age * 0.13;
		double radius = 0.48 + tier * 0.12;
		PowerFx.ring(level, point, radius, tier >= 4 ? WHITE : EMBER,
				10 + tier * 2, phase);
		PowerFx.ring(level, point.add(0.0, 0.10, 0.0), radius * 0.64,
				GOLD, 8 + tier * 2, -phase * 1.4);
		PowerFx.clarityBurst(level, point, afterimage ? PowersParticles.RIBBON : ParticleTypes.FLAME,
				2 + tier, 0.18 + tier * 0.03, 0.025);
		if (trueSight) {
			PowerFx.clarityBurst(level, point.add(0.0, 0.25, 0.0), PowersParticles.GLYPH,
					2, 0.12, 0.015);
		}
		if (ancientMastery && age % 40 == 0) {
			PowerFx.sound(level, point, PowersSounds.DARK_WHISPER, 0.28F, 1.35F);
		}
	}

	/** Tears the first launch seal open along the authoritative attack direction. */
	public static void launch(ServerLevel level, Vec3 point, Vec3 velocity, int tier,
			boolean ancientMastery) {
		Vec3 direction = velocity.lengthSqr() > 1.0E-8 ? velocity.normalize() : Vec3.ZERO;
		PowerFx.rune(level, point, 1.05 + tier * 0.22, GOLD,
				22 + tier * 4, Math.PI);
		PowerFx.beam(level, point.subtract(direction.scale(1.2)),
				point.add(direction.scale(1.8)), PowersParticles.RIBBON, 16 + tier * 2);
		PowerFx.clarityBurst(level, point, ParticleTypes.FLAME, 20 + tier * 6, 0.48, 0.25);
		PowerFx.clarityBurst(level, point, com.powers.PowersParticles.RIBBON, 8 + tier * 3, 0.36, 0.18);
		PowerFx.sound(level, point, SoundEvents.FIREWORK_ROCKET_SHOOT,
				0.9F + tier * 0.12F, ancientMastery ? 0.72F : 0.88F);
		PowerFx.sound(level, point, PowersSounds.INTERACTION_CLASH, 0.55F, 1.32F);
	}

	/** Draws a measured, tier-coloured flight wake between observed server positions. */
	public static void wake(ServerLevel level, Vec3 from, Vec3 to, int segments,
			int tier, int age, boolean afterimage, boolean trueSight) {
		if (segments <= 0) return;
		PowerFx.beam(level, from, to, PowersParticles.RIBBON, segments);
		PowerFx.beam(level, from.add(0.0, 0.12, 0.0), to.add(0.0, 0.12, 0.0),
				ParticleTypes.FLAME, Math.max(2, segments / 2));
		PowerFx.clarityBurst(level, to, tier >= 3 ? PowersParticles.SPARK : ParticleTypes.SMOKE,
				2 + tier, 0.18 + tier * 0.035, 0.055);
		if (afterimage && age % 4 == 0) {
			PowerFx.ring(level, from, 0.34 + tier * 0.08, COAL, 8 + tier * 2, age * 0.4);
		}
		if (trueSight && age % 6 == 0) {
			PowerFx.ring(level, to, 0.42 + tier * 0.07, GOLD, 10 + tier * 2, -age * 0.3);
		}
	}

	/** Shows control passing to a permitted reflector without hiding projectile ownership. */
	public static void reflected(ServerLevel level, Vec3 point, Vec3 velocity,
			int reflection, int limit) {
		Vec3 direction = velocity.lengthSqr() > 1.0E-8 ? velocity.normalize() : Vec3.ZERO;
		PowerFx.rune(level, point, 0.88 + reflection * 0.12, WHITE,
				18 + reflection * 4, reflection * Math.PI / 3.0);
		PowerFx.ring(level, point.add(0.0, 0.10, 0.0), 0.58 + reflection * 0.08,
				GOLD, 14 + reflection * 3, -reflection);
		PowerFx.beam(level, point.subtract(direction.scale(0.9)),
				point.add(direction.scale(1.2)), PowersParticles.FRACTURE, 12);
		PowerFx.clarityBurst(level, point, ParticleTypes.ELECTRIC_SPARK,
				12 + reflection * 3, 0.42, 0.13);
		PowerFx.sound(level, point, PowersSounds.INTERACTION_CLASH, 0.75F,
				1.48F - Math.min(0.42F, reflection * 0.11F));
		if (reflection >= limit) PowerFx.sound(level, point, PowersSounds.RUNE_HUM, 0.5F, 0.62F);
	}

	/** Seals an attempted reflection after its finite volley budget is exhausted. */
	public static void reflectionDenied(ServerLevel level, Vec3 point, int reflections) {
		PowerFx.rune(level, point, 0.94, COAL, 20, reflections * Math.PI / 4.0);
		PowerFx.ring(level, point.add(0.0, 0.12, 0.0), 0.62, EMBER,
				16, -level.getGameTime() * 0.2);
		PowerFx.clarityBurst(level, point, PowersParticles.FRACTURE, 18, 0.48, 0.10);
		PowerFx.sound(level, point, SoundEvents.BEACON_DEACTIVATE, 0.72F, 0.62F);
	}

	/** Gives every non-detonating impact terminal a distinct counter-seal. */
	public static void terminal(ServerLevel level, Vec3 point,
			FireballRules.ImpactDecision decision, int tier) {
		if (decision == null || decision == FireballRules.ImpactDecision.DETONATE
				|| decision == FireballRules.ImpactDecision.WATER
				|| decision == FireballRules.ImpactDecision.FROST) return;
		int primary = switch (decision) {
			case UNOWNED -> 0x8A8F96;
			case SAFE_ZONE -> 0x58C7FF;
			case AMETHYST -> 0xB36BFF;
			case SANCTUARY -> 0x8CFF98;
			case KINETIC_WARD, FORCEFIELD -> 0x40C4FF;
			case DETONATE, WATER, FROST -> EMBER;
		};
		PowerFx.rune(level, point, 0.88 + tier * 0.12, primary,
				18 + tier * 3, level.getGameTime() * 0.15);
		PowerFx.ring(level, point.add(0.0, 0.12, 0.0), 0.58 + tier * 0.08,
				WHITE, 14 + tier * 2, -level.getGameTime() * 0.22);
		PowerFx.clarityBurst(level, point,
				decision == FireballRules.ImpactDecision.AMETHYST
						? ParticleTypes.ELECTRIC_SPARK : PowersParticles.FRACTURE,
				14 + tier * 2, 0.46, 0.09);
		PowerFx.sound(level, point, switch (decision) {
			case AMETHYST -> PowersSounds.AMETHYST_FRACTURE;
			case SANCTUARY, KINETIC_WARD, FORCEFIELD, SAFE_ZONE -> PowersSounds.WARD_IMPACT;
			case UNOWNED -> SoundEvents.BEACON_DEACTIVATE;
			case DETONATE, WATER, FROST -> PowersSounds.INTERACTION_CLASH;
		}, 0.72F, decision == FireballRules.ImpactDecision.UNOWNED ? 0.65F : 0.92F);
	}

	/** Transforms opposed water or frost into a no-ignition pressure cloud. */
	public static void steam(ServerLevel level, Vec3 point, double radius,
			boolean frost, int tier) {
		PowerFx.rune(level, point, Math.min(4.8, radius), frost ? 0x82E9FF : STEAM,
				28 + tier * 4, level.getGameTime() * 0.16);
		PowerFx.ring(level, point.add(0.0, 0.12, 0.0), Math.min(3.8, radius * 0.72),
				WHITE, 22 + tier * 3, -level.getGameTime() * 0.22);
		PowerFx.clarityBurst(level, point.add(0.0, 0.5, 0.0), com.powers.PowersParticles.RIBBON,
				28 + tier * 8, Math.min(2.2, radius * 0.48), 0.16);
		PowerFx.clarityBurst(level, point.add(0.0, 0.5, 0.0), ParticleTypes.GUST,
				8 + tier * 2, Math.min(1.5, radius * 0.32), 0.13);
		PowerFx.sound(level, point, SoundEvents.FIRE_EXTINGUISH, 1.15F, frost ? 0.72F : 0.88F);
		PowerFx.sound(level, point, PowersSounds.INTERACTION_CLASH, 0.82F, 1.28F);
	}

	/** Resolves an ordinary or Empowered Cinderheart detonation without terrain explosion. */
	public static void impact(ServerLevel level, Vec3 point, double radius, int tier,
			int affectedTargets, boolean empoweredImpact, boolean ancientMastery) {
		double outer = Math.min(5.5, radius);
		PowerFx.rune(level, point, outer, tier >= 4 ? WHITE : EMBER,
				32 + tier * 5, level.getGameTime() * 0.16);
		PowerFx.ring(level, point.add(0.0, 0.14, 0.0), outer * 0.72,
				empoweredImpact ? GOLD : WHITE, 26 + tier * 4,
				-level.getGameTime() * 0.24);
		PowerFx.ring(level, point.add(0.0, 0.28, 0.0), outer * 0.42,
				COAL, 20 + tier * 3, level.getGameTime() * 0.32);
		PowerFx.clarityBurst(level, point.add(0.0, 0.6, 0.0), ParticleTypes.EXPLOSION,
				2 + tier, 0.55 + tier * 0.12, 0.08);
		PowerFx.clarityBurst(level, point.add(0.0, 0.7, 0.0), ParticleTypes.FLAME,
				26 + tier * 10, Math.min(2.4, radius * 0.48), 0.24);
		PowerFx.clarityBurst(level, point.add(0.0, 0.7, 0.0), PowersParticles.FRACTURE,
				18 + tier * 6, Math.min(2.0, radius * 0.42), 0.17);
		if (ancientMastery) {
			PowerFx.clarityBurst(level, point.add(0.0, 0.8, 0.0), PowersParticles.GLYPH,
					12 + Math.min(16, affectedTargets), 1.1, 0.10);
		}
		PowerFx.sound(level, point, SoundEvents.GENERIC_EXPLODE.value(),
				1.0F + tier * 0.18F, 1.12F - tier * 0.12F);
		PowerFx.sound(level, point, empoweredImpact
				? SoundEvents.WARDEN_SONIC_BOOM : PowersSounds.INTERACTION_CLASH,
				empoweredImpact ? 1.05F : 0.68F, empoweredImpact ? 1.28F : 0.82F);
	}

	/** Marks one consent-safe body receiving the Empowered or steam pressure wave. */
	public static void pressureTarget(ServerLevel level, Vec3 from, Vec3 target,
			boolean steam, int index) {
		PowerFx.beam(level, from.add(0.0, 0.5, 0.0), target,
				steam ? PowersParticles.MOTE : PowersParticles.RIBBON, 7);
		PowerFx.ring(level, target, 0.42 + index * 0.025,
				steam ? STEAM : GOLD, 10, index * 0.55);
	}

	/** Marks one server-authorized surface flame without resembling body pressure. */
	public static void terrainScorch(ServerLevel level, Vec3 point, int index) {
		PowerFx.ring(level, point.add(0.0, 0.04, 0.0), 0.28 + index * 0.018,
				EMBER, 8, index * 0.48);
		PowerFx.clarityBurst(level, point.add(0.0, 0.18, 0.0), ParticleTypes.FLAME,
				3, 0.12, 0.035);
		PowerFx.clarityBurst(level, point.add(0.0, 0.14, 0.0), PowersParticles.SPARK,
				2, 0.10, 0.025);
	}

	/** Collapses an invalid, interrupted, or expired heart without an impact. */
	public static void extinguish(ServerLevel level, Vec3 point, int tier,
			boolean expired, boolean amethyst, boolean frozen) {
		int color = amethyst ? 0xB36BFF : frozen ? 0xE8FFFF : expired ? COAL : 0x8A8F96;
		PowerFx.rune(level, point, 0.72 + tier * 0.12, color,
				16 + tier * 3, Math.PI);
		PowerFx.clarityBurst(level, point, amethyst ? ParticleTypes.ELECTRIC_SPARK
				: expired ? ParticleTypes.SMOKE : PowersParticles.FRACTURE,
				12 + tier * 3, 0.42, 0.065);
		PowerFx.sound(level, point, amethyst ? PowersSounds.AMETHYST_FRACTURE
				: frozen ? PowersSounds.TIME_SUSPEND : SoundEvents.FIRE_EXTINGUISH,
				0.68F, expired ? 0.72F : 0.92F);
	}

	/** Refuses a cast whose complete one-block orb volume has no valid spawn cell. */
	public static void blocked(ServerLevel level, Vec3 point) {
		PowerFx.rune(level, point, 0.68, COAL, 14, Math.PI);
		PowerFx.clarityBurst(level, point, ParticleTypes.SMOKE, 12, 0.34, 0.04);
		PowerFx.clarityBurst(level, point, PowersParticles.FRACTURE, 8, 0.28, 0.035);
		PowerFx.sound(level, point, SoundEvents.BEACON_DEACTIVATE, 0.52F, 0.76F);
	}
}

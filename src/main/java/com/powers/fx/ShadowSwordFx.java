package com.powers.fx;

import com.powers.PowersParticles;
import com.powers.PowersSounds;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/** Bounded violet-black presentation shared by every Shadow Sword invocation. */
public final class ShadowSwordFx {
	private static final int VOID_VIOLET = 0x3A0B52;
	private static final int DEAD_MAGENTA = 0x7B173E;

	private ShadowSwordFx() {
	}

	/** Corrupts an existing power's readable silhouette without obscuring it. */
	public static void corruptedCast(ServerLevel level, Vec3 origin, long phaseSeed) {
		double phase = Math.floorMod(phaseSeed, 360) * Math.PI / 180.0;
		PowerFx.rune(level, origin.add(0.0, 0.08, 0.0), 1.25, VOID_VIOLET, 20, phase);
		PowerFx.ring(level, origin.add(0.0, 1.05, 0.0), 0.72, DEAD_MAGENTA, 14, -phase);
		PowerFx.burst(level, origin.add(0.0, 1.0, 0.0), PowersParticles.ECLIPSE, 8, 0.42, 0.025);
		PowerFx.burst(level, origin.add(0.0, 1.0, 0.0), ParticleTypes.REVERSE_PORTAL, 6, 0.35, 0.01);
		PowerFx.sound(level, origin, PowersSounds.DARK_WHISPER, 0.85F, 0.55F);
	}

	public static void guardianArrival(ServerLevel level, Vec3 position) {
		PowerFx.rune(level, position.add(0.0, 0.08, 0.0), 1.4, VOID_VIOLET, 24, 0.0);
		PowerFx.spiral(level, position, 0.55, 2.2, DEAD_MAGENTA, 18, Math.PI);
		PowerFx.burst(level, position.add(0.0, 1.0, 0.0), ParticleTypes.REVERSE_PORTAL, 18, 0.65, 0.035);
		PowerFx.sound(level, position, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2F, 0.55F);
	}

	public static void spread(ServerLevel level, Vec3 origin, int changed) {
		PowerFx.rune(level, origin.add(0.0, 0.08, 0.0), 6.0, VOID_VIOLET, 42, 0.0);
		PowerFx.ring(level, origin.add(0.0, 0.12, 0.0), 3.0, DEAD_MAGENTA, 28, Math.PI);
		PowerFx.burst(level, origin.add(0.0, 0.5, 0.0), PowersParticles.ROOT,
				Math.min(32, 8 + changed / 4), 2.8, 0.04);
		PowerFx.sound(level, origin, PowersSounds.DARK_WHISPER, 1.5F, 0.35F);
	}

	public static void singularity(ServerLevel level, Vec3 center, boolean detonation) {
		PowerFx.rune(level, center, detonation ? 8.0 : 5.0, VOID_VIOLET,
				detonation ? 52 : 36, detonation ? Math.PI : 0.0);
		PowerFx.spiral(level, center, detonation ? 4.0 : 2.4, detonation ? 6.0 : 4.0,
				0x120018, detonation ? 44 : 28, level.getGameTime() * 0.08);
		PowerFx.burst(level, center, detonation ? ParticleTypes.EXPLOSION : ParticleTypes.REVERSE_PORTAL,
				detonation ? 10 : 32, detonation ? 2.0 : 4.0, detonation ? 0.15 : 0.03);
		PowerFx.sound(level, center, detonation ? SoundEvents.GENERIC_EXPLODE.value()
				: PowersSounds.DARK_WHISPER, detonation ? 2.4F : 1.6F, detonation ? 0.35F : 0.25F);
	}

	public static void annihilationBeam(ServerLevel level, Vec3 from, Vec3 to) {
		PowerFx.beam(level, from, to, ColorParticleOption.create(
				ParticleTypes.ENTITY_EFFECT, 0xFF120018), 72);
		PowerFx.beam(level, from, to, PowersParticles.FRACTURE, 48);
		PowerFx.rune(level, from, 1.7, DEAD_MAGENTA, 26, 0.0);
		PowerFx.rune(level, to, 3.2, VOID_VIOLET, 34, Math.PI);
		PowerFx.burst(level, to, ParticleTypes.EXPLOSION, 8, 1.4, 0.08);
		PowerFx.sound(level, from, PowersSounds.DARK_WHISPER, 1.8F, 0.22F);
		PowerFx.sound(level, to, SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.45F);
	}

	public static void dominion(ServerLevel level, Vec3 center, long tick, boolean beginning) {
		double phase = tick * 0.07;
		PowerFx.ring(level, center.add(0.0, 0.1, 0.0), beginning ? 7.0 : 3.5,
				VOID_VIOLET, beginning ? 44 : 22, phase);
		PowerFx.ring(level, center.add(0.0, 1.3, 0.0), beginning ? 4.0 : 2.2,
				DEAD_MAGENTA, beginning ? 32 : 16, -phase);
		if (beginning) {
			PowerFx.spiral(level, center, 1.6, 5.0, 0x120018, 36, phase);
			PowerFx.sound(level, center, PowersSounds.DARK_WHISPER, 2.0F, 0.25F);
		}
	}
}

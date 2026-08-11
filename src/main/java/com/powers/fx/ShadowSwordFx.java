package com.powers.fx;

import com.powers.PowersParticles;
import com.powers.PowersSounds;
import com.powers.item.ShadowSwordPalette;
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
		corruptedCast(level, origin, phaseSeed, 0x55265F);
	}

	/** Adds a darkened echo of the invoked power's original colour. */
	public static void corruptedCast(ServerLevel level, Vec3 origin, long phaseSeed, int originalColor) {
		double phase = Math.floorMod(phaseSeed, 360) * Math.PI / 180.0;
		ShadowSwordPalette.Palette palette = ShadowSwordPalette.corrupt(originalColor);
		PowerFx.rune(level, origin.add(0.0, 0.08, 0.0), 1.25, palette.primary(), 20, phase);
		PowerFx.ring(level, origin.add(0.0, 1.05, 0.0), 0.72, palette.secondary(), 14, -phase);
		PowerFx.burst(level, origin.add(0.0, 1.0, 0.0), PowersParticles.ECLIPSE, 8, 0.42, 0.025);
		PowerFx.burst(level, origin.add(0.0, 1.0, 0.0), com.powers.PowersParticles.ECLIPSE, 6, 0.35, 0.01);
		PowerFx.sound(level, origin, PowersSounds.DARK_WHISPER, 0.85F, 0.55F);
	}

	/**
	 * Reuses the retired singularity, requiem, and annihilation silhouettes as
	 * one bounded apotheosis ceremony instead of exposing them as menu actions.
	 */
	public static void dominion(ServerLevel level, Vec3 center, long tick, boolean beginning) {
		double phase = tick * 0.07;
		PowerFx.ring(level, center.add(0.0, 0.1, 0.0), beginning ? 7.0 : 3.5,
				VOID_VIOLET, beginning ? 44 : 22, phase);
		PowerFx.ring(level, center.add(0.0, 1.3, 0.0), beginning ? 4.0 : 2.2,
				DEAD_MAGENTA, beginning ? 32 : 16, -phase);
		if (beginning) {
			PowerFx.spiral(level, center, 1.6, 5.0, 0x120018, 36, phase);
			PowerFx.rune(level, center.add(0.0, 0.08, 0.0), 7.5, 0x19051F, 48, -phase);
			PowerFx.burst(level, center.add(0.0, 1.0, 0.0),
					PowersParticles.FRACTURE, 18, 2.4, 0.06);
			PowerFx.sound(level, center, SoundEvents.WARDEN_SONIC_BOOM, 1.8F, 0.3F);
			PowerFx.sound(level, center, PowersSounds.DARK_WHISPER, 2.0F, 0.25F);
		}
	}
}

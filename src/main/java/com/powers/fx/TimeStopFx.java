package com.powers.fx;

import com.powers.PowersParticles;
import com.powers.PowersSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/** Shared clock-face cues for toggle and crystal time-stop presentations. */
public final class TimeStopFx {
	private static final int TOGGLE_CYAN = 0x96F5FF;
	private static final int CRYSTAL_BLUE = 0x2962FF;
	private static final int FROZEN_WHITE = 0xE8FFFF;

	private TimeStopFx() {
	}

	/** Opens an exact-radius stasis boundary and a compact clock around its owner. */
	public static void begin(ServerLevel level, Vec3 center, double radius, boolean crystal) {
		int color = crystal ? CRYSTAL_BLUE : TOGGLE_CYAN;
		double inner = crystal ? 3.4 : 2.4;
		PowerFx.ring(level, center.add(0.0, 0.08, 0.0), radius, color,
				boundaryPoints(radius), 0.0);
		PowerFx.rune(level, center.add(0.0, 0.12, 0.0), inner, FROZEN_WHITE,
				crystal ? 32 : 24, Math.PI / 12.0);
		PowerFx.spiral(level, center.subtract(0.0, 0.45, 0.0), inner * 0.36,
				2.4, color, crystal ? 20 : 14, 0.0);
		PowerFx.clarityBurst(level, center.add(0.0, 1.0, 0.0),
				ParticleTypes.REVERSE_PORTAL, crystal ? 20 : 12, 0.65, 0.015);
		PowerFx.sound(level, center, PowersSounds.TIME_SUSPEND,
				crystal ? 1.15F : 0.82F, crystal ? 0.72F : 0.9F);
		PowerFx.sound(level, center, SoundEvents.BEACON_POWER_SELECT,
				crystal ? 0.72F : 0.48F, 1.65F);
	}

	/** Keeps the outer boundary legible with a sparse, low-camera inner pulse. */
	public static void sustain(ServerLevel level, Vec3 center, double radius,
			long tick, boolean crystal) {
		int color = crystal ? CRYSTAL_BLUE : TOGGLE_CYAN;
		double phase = tick * (crystal ? 0.055 : 0.08);
		PowerFx.ring(level, center.add(0.0, 0.08, 0.0), radius, color,
				boundaryPoints(radius), phase);
		PowerFx.ring(level, center.add(0.0, 0.16, 0.0), crystal ? 3.1 : 2.1,
				FROZEN_WHITE, crystal ? 24 : 18, -phase * 1.4);
		PowerFx.clarityBurst(level, center.add(0.0, 0.45, 0.0),
				crystal ? PowersParticles.FRACTURE : ParticleTypes.SOUL,
				crystal ? 5 : 3, crystal ? 0.65 : 0.35, 0.018);
		if (tick % 40 == 0) {
			PowerFx.sound(level, center, PowersSounds.RUNE_HUM, 0.28F,
					crystal ? 0.64F : 0.82F);
		}
	}

	/** Reverses the clock geometry and gives resumed time its own release sound. */
	public static void release(ServerLevel level, Vec3 center, double radius,
			boolean crystal) {
		int color = crystal ? CRYSTAL_BLUE : TOGGLE_CYAN;
		PowerFx.ring(level, center.add(0.0, 0.08, 0.0), radius, FROZEN_WHITE,
				boundaryPoints(radius), Math.PI);
		PowerFx.rune(level, center.add(0.0, 0.12, 0.0), crystal ? 3.0 : 2.0,
				color, crystal ? 28 : 20, -Math.PI / 10.0);
		PowerFx.clarityBurst(level, center.add(0.0, 0.75, 0.0),
				ParticleTypes.REVERSE_PORTAL, crystal ? 18 : 10, 0.7, 0.12);
		PowerFx.clarityBurst(level, center.add(0.0, 0.55, 0.0),
				PowersParticles.FRACTURE, crystal ? 14 : 8, 0.55, 0.08);
		PowerFx.sound(level, center, PowersSounds.TIME_RELEASE,
				crystal ? 1.0F : 0.72F, crystal ? 1.18F : 1.34F);
	}

	/**
	 * Announces a true server-wide stop around one observer. A ground clock and
	 * high fractured halo make the global scope readable without filling the
	 * camera with dense particles.
	 */
	public static void globalBegin(ServerLevel level, Vec3 observer, boolean crystal) {
		int color = crystal ? CRYSTAL_BLUE : TOGGLE_CYAN;
		PowerFx.ring(level, observer.add(0.0, 0.08, 0.0), 5.0,
				color, crystal ? 44 : 32, 0.0);
		PowerFx.rune(level, observer.add(0.0, 10.0, 0.0), 8.0,
				FROZEN_WHITE, 40, Math.PI / 12.0);
		PowerFx.spiral(level, observer.add(0.0, 0.2, 0.0), 1.0,
				8.0, color, crystal ? 28 : 18, 0.0);
		PowerFx.sound(level, observer, PowersSounds.TIME_SUSPEND, 1.0F, 0.72F);
		PowerFx.sound(level, observer, SoundEvents.BEACON_ACTIVATE, 0.6F, 1.65F);
	}

	/** Sparse once-per-second clock pulses visible to observers in every dimension. */
	public static void globalSustain(ServerLevel level, Vec3 observer, long tick,
			boolean crystal) {
		int color = crystal ? CRYSTAL_BLUE : TOGGLE_CYAN;
		double phase = tick * 0.035;
		PowerFx.ring(level, observer.add(0.0, 9.0, 0.0), 8.0,
				FROZEN_WHITE, 32, phase);
		PowerFx.ring(level, observer.add(0.0, 0.08, 0.0), 4.0,
				color, crystal ? 34 : 24, -phase * 1.7);
		PowerFx.clarityBurst(level, observer.add(0.0, 6.0, 0.0),
				PowersParticles.FRACTURE, 5, 1.6, 0.01);
		PowerFx.sound(level, observer, PowersSounds.RUNE_HUM, 0.26F, 0.58F);
	}

	/** Reverses the global clock around each observer when ordinary time resumes. */
	public static void globalRelease(ServerLevel level, Vec3 observer, boolean crystal) {
		int color = crystal ? CRYSTAL_BLUE : TOGGLE_CYAN;
		PowerFx.ring(level, observer.add(0.0, 0.08, 0.0), 6.0,
				FROZEN_WHITE, 36, Math.PI);
		PowerFx.rune(level, observer.add(0.0, 8.0, 0.0), 6.0,
				color, crystal ? 42 : 32, -Math.PI / 9.0);
		PowerFx.clarityBurst(level, observer.add(0.0, 1.0, 0.0),
				PowersParticles.FRACTURE, 12, 1.2, 0.08);
		PowerFx.sound(level, observer, PowersSounds.TIME_RELEASE, 0.86F, 1.22F);
	}

	static int boundaryPoints(double radius) {
		return Math.max(24, Math.min(56, (int) Math.ceil(Math.max(0.0, radius) * 2.0)));
	}
}

package com.powers.fx;

import com.powers.PowersSounds;
import com.powers.network.CelestialRuinPackets;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/** Bounded audiovisual choreography for the three phases of Heavenfall. */
public final class CelestialRuinFx {
	private CelestialRuinFx() {
	}

	/** Opens the irreversible warning circle. */
	public static void begins(ServerLevel level, Vec3 center, int beamRadius) {
		CelestialRuinPackets.broadcast(level, center, CelestialRuinPackets.Phase.BEGIN, 0);
		PowerFx.rune(level, center.add(0.0, 0.08, 0.0), beamRadius, 0xFFF7D6, 96, 0.0);
		PowerFx.rune(level, center.add(0.0, 0.12, 0.0), beamRadius * 0.62,
				0x8DEBFF, 64, Math.PI / 8.0);
		PowerFx.burst(level, center, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF),
				4, 2.0, 0.0);
		PowerFx.sound(level, center, PowersSounds.LIGHT_CHORUS, 4.0F, 0.45F);
	}

	/** Draws a pulsing hundred-block-wide sky column without unbounded particles. */
	public static void beam(ServerLevel level, Vec3 center, int beamRadius, int age) {
		if (age % com.powers.spell.CelestialRuinPresentation.BEAM_REFRESH_TICKS == 0) {
			CelestialRuinPackets.broadcast(level, center, CelestialRuinPackets.Phase.SUSTAIN, age);
		}
		double pulse = 0.92 + 0.08 * Math.sin(age * 0.08);
		double radius = beamRadius * pulse;
		double phase = age * 0.012;
		for (int band = 0; band < 4; band++) {
			double y = Math.min(level.getMaxY() - 2.0, center.y + band * 42.0);
			PowerFx.ring(level, new Vec3(center.x, y, center.z), radius,
					0xFFF7D6, 40, phase + band * 0.2);
		}
		for (int ray = 0; ray < 12; ray++) {
			double angle = Math.PI * 2.0 * ray / 12.0 + phase;
			Vec3 from = center.add(Math.cos(angle) * radius, 0.5, Math.sin(angle) * radius);
			Vec3 to = new Vec3(from.x,
					Math.min(level.getMaxY() - 2.0, center.y + 126.0), from.z);
			PowerFx.beam(level, from, to, com.powers.PowersParticles.GLYPH, 6);
		}
		if (age % 20 == 0) {
			PowerFx.sound(level, center, PowersSounds.RUNE_HUM,
					1.8F, 0.52F + (age % 200) / 1000.0F);
		}
	}

	/** Releases the lethal light and first expanding ruin wave. */
	public static void detonates(ServerLevel level, Vec3 center, int blastRadius) {
		CelestialRuinPackets.broadcast(level, center, CelestialRuinPackets.Phase.DETONATE, 0);
		PowerFx.burst(level, center, ParticleTypes.EXPLOSION_EMITTER, 8, 3.0, 0.0);
		PowerFx.burst(level, center.add(0.0, 4.0, 0.0),
				ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), 10, 12.0, 0.0);
		PowerFx.rune(level, center, Math.min(blastRadius, 64), 0xFFFFFF, 128, 0.0);
		PowerFx.sound(level, center, SoundEvents.GENERIC_EXPLODE.value(), 12.0F, 0.35F);
		PowerFx.sound(level, center, PowersSounds.LIGHT_CHORUS, 8.0F, 0.25F);
	}

	/** Seals the final terrain batch with an inward celestial fracture. */
	public static void finished(ServerLevel level, Vec3 center, int blastRadius) {
		CelestialRuinPackets.broadcast(level, center, CelestialRuinPackets.Phase.END, 0);
		PowerFx.rune(level, center, Math.min(blastRadius, 48), 0x8DEBFF, 96, Math.PI);
		PowerFx.burst(level, center, com.powers.PowersParticles.ECLIPSE, 64, 8.0, 0.25);
		PowerFx.sound(level, center, PowersSounds.RIFT_CLOSE, 4.0F, 0.4F);
	}
}

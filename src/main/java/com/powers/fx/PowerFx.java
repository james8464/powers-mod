package com.powers.fx;

import com.powers.PowersParticles;
import com.powers.PowersSounds;
import com.powers.config.PowersConfigLoader;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * server-side visual and audio helpers shared by every ability. they give
 * each power an identity beyond the raw mechanic: colored bursts, beams,
 * trails and cast sounds
 */
public final class PowerFx {
	private static final java.util.Map<MinecraftServer, ParticleBudget> BUDGETS = new java.util.WeakHashMap<>();

	private PowerFx() {
	}

	/** puffs a cloud of particles around a point */
	public static void burst(ServerLevel level, Vec3 pos, ParticleOptions particle, int count, double spread, double speed) {
		int limit = PowersConfigLoader.get().maxParticlesPerTick();
		MinecraftServer server = level.getServer();
		ParticleBudget budget = BUDGETS.get(server);
		if (budget == null || budget.limit() != limit) {
			budget = new ParticleBudget(limit);
			BUDGETS.put(server, budget);
		}
		int granted = budget.claim(server.getTickCount(), count);
		if (granted > 0) level.sendParticles(particle, pos.x, pos.y, pos.z, granted, spread, spread, spread, speed);
	}

	/** puffs a cloud of particles tinted with an rgb color */
	public static void coloredBurst(ServerLevel level, Vec3 pos, int rgb, int count, double spread) {
		burst(level, pos, ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | (rgb & 0xFFFFFF)), count, spread, 0.0);
	}

	/** draws a straight line of particles between two points */
	public static void beam(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle, int steps) {
		Vec3 delta = to.subtract(from);
		for (int i = 1; i <= steps; i++) {
			Vec3 point = from.add(delta.scale((double) i / steps));
			burst(level, point, particle, 1, 0.04, 0.0);
		}
	}

	/** draws a flat magic circle; the phase makes it look like it slowly rotates */
	public static void ring(ServerLevel level, Vec3 center, double radius, int rgb, int points, double phase) {
		for (int i = 0; i < points; i++) {
			double angle = Math.PI * 2.0 * i / points + phase;
			Vec3 point = center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
			coloredBurst(level, point, rgb, 1, 0.015);
		}
	}

	/** draws a circle of rune sparks with a faint inner ring */
	public static void rune(ServerLevel level, Vec3 center, double radius, int rgb, int points, double phase) {
		ring(level, center, radius, rgb, points, phase);
		for (int i = 0; i < points; i++) {
			double angle = Math.PI * 2.0 * i / points + phase;
			Vec3 point = center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
			burst(level, point.add(0, 0.15, 0), ParticleTypes.END_ROD, 1, 0.08, 0.02);
		}
		spiral(level, center, radius * 0.55, radius * 0.4, rgb, Math.max(6, points / 2), phase + Math.PI / 8);
	}

	/** draws a short rising spiral for transformations and charged casts */
	public static void spiral(ServerLevel level, Vec3 center, double radius, double height,
			int rgb, int points, double phase) {
		for (int i = 0; i < points; i++) {
			double progress = i / (double) Math.max(1, points - 1);
			double angle = phase + progress * Math.PI * 4.0;
			Vec3 point = center.add(Math.cos(angle) * radius, progress * height, Math.sin(angle) * radius);
			coloredBurst(level, point, rgb, 1, 0.015);
		}
	}

	/** plays a sound to everyone around a point */
	public static void sound(ServerLevel level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
	}

	// the small "no" burst for a cancelled or refused cast
	public static void cancelled(ServerLevel level, Vec3 pos, int rgb) {
		burst(level, pos, ParticleTypes.REVERSE_PORTAL, 10, 0.35, 0.02);
		coloredBurst(level, pos, rgb, 8, 0.25);
		sound(level, pos, SoundEvents.BEACON_DEACTIVATE, 0.5f, 0.7f);
	}

	// two colored beams meeting mid-air with a spark burst, for powers clashing
	public static void clash(ServerLevel level, Vec3 from, Vec3 to, int attacker, int defender) {
		Vec3 midpoint = from.add(to).scale(0.5);
		beam(level, from, midpoint, ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | attacker), 8);
		beam(level, to, midpoint, ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | defender), 8);
		burst(level, midpoint, ParticleTypes.ELECTRIC_SPARK, 16, 0.4, 0.08);
		sound(level, midpoint, SoundEvents.BEACON_DEACTIVATE, 0.8f, 1.4f);
	}

	/** Emits the first catastrophic eclipse flash when living light and darkness touch. */
	public static void forceClashDetonation(ServerLevel level, Vec3 center, int radius) {
		burst(level, center, ParticleTypes.EXPLOSION_EMITTER, 2, 0.1, 0.0);
		burst(level, center, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), 12, 1.2, 0.0);
		burst(level, center, PowersParticles.FRACTURE, 48, 3.5, 0.35);
		burst(level, center, PowersParticles.ECLIPSE, 40, 2.6, 0.22);
		rune(level, center, Math.min(8.0, radius * 0.2), 0xFFF4C7, 48, 0.0);
		rune(level, center.add(0, 0.15, 0), Math.min(6.5, radius * 0.16), 0x2A0C3D, 40, Math.PI / 16.0);
		sound(level, center, PowersSounds.LIGHT_CHORUS, 8.0F, 0.5F);
		sound(level, center, PowersSounds.DARK_WHISPER, 8.0F, 0.5F);
		sound(level, center, PowersSounds.INTERACTION_CLASH, 12.0F, 0.35F);
		sound(level, center, SoundEvents.GENERIC_EXPLODE.value(), 12.0F, 0.5F);
		sound(level, center, SoundEvents.END_PORTAL_SPAWN, 6.0F, 0.55F);
	}

	/** Draws the expanding, alternating corona of an active annihilation wave. */
	public static void forceClashWave(ServerLevel level, Vec3 center, double radius, int age) {
		double phase = age * 0.16;
		int points = Math.max(16, Math.min(64, (int) Math.ceil(radius * 2.0)));
		ring(level, center, radius, 0xFFF4C7, points, phase);
		ring(level, center.add(0, 0.12, 0), Math.max(0.5, radius - 0.55), 0x2A0C3D, points, -phase);
		if (age % 4 == 0) {
			burst(level, center, ParticleTypes.END_ROD, 8, Math.min(radius, 10.0), 0.05);
			burst(level, center, ParticleTypes.LARGE_SMOKE, 8, Math.min(radius, 10.0), 0.04);
		}
		if (age % 12 == 0) sound(level, center, PowersSounds.INTERACTION_CLASH, 3.0F, 0.65F);
	}

	/** Seals a completed clash with a final inward fracture and bass impact. */
	public static void forceClashFinished(ServerLevel level, Vec3 center, int radius) {
		burst(level, center, PowersParticles.FRACTURE, 32, Math.min(radius, 12.0), 0.12);
		burst(level, center, ParticleTypes.REVERSE_PORTAL, 40, Math.min(radius, 10.0), 0.16);
		rune(level, center, Math.min(radius, 12.0), 0xBFA8FF, 56, Math.PI / 8.0);
		sound(level, center, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 6.0F, 0.45F);
	}

	/** Shows whether darkness is poisoning or welcoming an entity. */
	public static void darknessAura(ServerLevel level, Vec3 center, boolean restorative) {
		burst(level, center, restorative ? PowersParticles.MOTE : PowersParticles.ECLIPSE,
				restorative ? 9 : 13, 0.65, restorative ? 0.035 : 0.075);
		coloredBurst(level, center, restorative ? 0x7C36C8 : 0x190522, restorative ? 8 : 12, 0.55);
		spiral(level, center.add(0, -0.45, 0), 0.55, 1.15,
				restorative ? 0xA456E8 : 0x2A0C3D, 10, level.getGameTime() * 0.12);
		if (level.getRandom().nextInt(8) == 0) {
			sound(level, center, PowersSounds.DARK_WHISPER, 0.45F, restorative ? 1.15F : 0.62F);
		}
	}

	/** Visual counter-cue when carried amethyst blocks darkness-fed restoration. */
	public static void amethystDarknessInterference(ServerLevel level, Vec3 center) {
		burst(level, center, ParticleTypes.ELECTRIC_SPARK, 10, 0.55, 0.08);
		burst(level, center, PowersParticles.FRACTURE, 8, 0.45, 0.04);
		coloredBurst(level, center, 0xB36BFF, 12, 0.5);
		if (level.getRandom().nextInt(5) == 0) {
			sound(level, center, PowersSounds.AMETHYST_FRACTURE, 0.5F, 0.85F);
		}
	}

	/** Draws the cyan-and-gold third-eye signature of a True Sight veil piercing. */
	public static void trueSightPiercing(ServerLevel level, Vec3 center) {
		for (int point = 0; point < 24; point++) {
			double angle = Math.PI * 2.0 * point / 24.0;
			Vec3 eye = center.add(Math.cos(angle) * 1.35, 0.08, Math.sin(angle) * 0.52);
			coloredBurst(level, eye, 0x8FE9FF, 1, 0.015);
		}
		rune(level, center.add(0.0, 0.12, 0.0), 0.36, 0xFFE08A, 12, Math.PI / 4.0);
		beam(level, center.add(0.0, 0.25, 0.0), center.add(0.0, 5.5, 0.0),
				ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF8FE9FF), 16);
		burst(level, center, ParticleTypes.ENCHANT, 14, 0.45, 0.05);
		sound(level, center, SoundEvents.AMETHYST_BLOCK_CHIME, 1.1F, 1.65F);
		sound(level, center, SoundEvents.CONDUIT_AMBIENT, 0.7F, 1.35F);
	}

	/** Marks a low-energy Darkness affinity pulse awakening the Dark Resurgence variant. */
	public static void darknessResurgence(ServerLevel level, Vec3 center) {
		burst(level, center, ParticleTypes.SOUL_FIRE_FLAME, 12, 0.58, 0.075);
		burst(level, center, PowersParticles.ECLIPSE, 10, 0.48, 0.045);
		coloredBurst(level, center, 0xA456E8, 14, 0.62);
		rune(level, center.add(0.0, -0.42, 0.0), 0.78, 0x2A0C3D, 16,
				level.getGameTime() * 0.16);
		spiral(level, center.add(0.0, -0.55, 0.0), 0.48, 1.65, 0x7C36C8, 14,
				level.getGameTime() * 0.12);
		if (level.getRandom().nextInt(4) == 0) {
			sound(level, center, PowersSounds.DARK_WHISPER, 0.65F, 1.22F);
			sound(level, center, SoundEvents.SOUL_ESCAPE.value(), 0.45F, 0.72F);
		}
	}

	/** a cycling rainbow rgb color, for rainbow steve's effects */
	public static int rainbow(int tick, int step) {
		float hue = (float) ((tick * step) % 360) / 60.0f;
		float x = 1.0f - Math.abs(hue % 2.0f - 1.0f);
		float r;
		float g;
		float b;
		if (hue < 1) {
			r = 1.0f;
			g = x;
			b = 0.0f;
		} else if (hue < 2) {
			r = x;
			g = 1.0f;
			b = 0.0f;
		} else if (hue < 3) {
			r = 0.0f;
			g = 1.0f;
			b = x;
		} else if (hue < 4) {
			r = 0.0f;
			g = x;
			b = 1.0f;
		} else if (hue < 5) {
			r = x;
			g = 0.0f;
			b = 1.0f;
		} else {
			r = 1.0f;
			g = 0.0f;
			b = x;
		}
		return ((int) (r * 255.0f) << 16) | ((int) (g * 255.0f) << 8) | (int) (b * 255.0f);
	}

	public static void clearBudgets() {
		BUDGETS.clear();
	}
}

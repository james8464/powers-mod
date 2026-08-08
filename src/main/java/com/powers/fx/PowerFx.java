package com.powers.fx;

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

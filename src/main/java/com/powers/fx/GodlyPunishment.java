package com.powers.fx;

import com.powers.PowersMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * Dramatic, god-like punishment effects. Where PowerFx provides the everyday
 * ability visuals, these sequences are reserved for moments of judgement:
 * burnt-out toggles, amethyst rejection, and broken travel. They paint a
 * picture of unseen powers looking down on the player.
 */
public final class GodlyPunishment {
	private GodlyPunishment() {
	}

	/**
	 * The full divine-wrath sequence: a rune circle and shockwave rings on the
	 * ground, a rising pillar of sparks, a burst of golden light, thunderous
	 * sounds, and — optionally — a lightning storm that chases the player.
	 * A delayed second wave lands shortly after, as if the judgement follows.
	 */
	public static void strike(ServerLevel level, ServerPlayer player, int rgb, boolean storm) {
		Vec3 pos = player.position().add(0, 1, 0);
		double phase = level.getServer().getTickCount() * 0.06;

		PowerFx.rune(level, pos.add(0, -0.3, 0), 3.0, rgb, 28, phase);
		PowerFx.ring(level, pos.add(0, -0.3, 0), 5.0, rgb, 36, phase + 0.35);
		PowerFx.spiral(level, pos, 1.6, 7.0, rgb, 42, 0.0);
		PowerFx.burst(level, pos, ParticleTypes.EXPLOSION, 26, 2.4, 0.3);
		PowerFx.coloredBurst(level, pos, rgb, 60, 1.6);
		PowerFx.burst(level, pos.add(0, 3, 0), ParticleTypes.END_ROD, 34, 0.6, 0.35);
		PowerFx.sound(level, pos, SoundEvents.BEACON_ACTIVATE, 1.0f, 0.5f);
		PowerFx.sound(level, pos, SoundEvents.GENERIC_EXPLODE.value(), 1.4f, 0.5f);
		PowerFx.sound(level, pos, SoundEvents.WITHER_SPAWN, 1.0f, 0.7f);
		if (storm) {
			PowersMod.startStorm(level, pos, player, 100, 100);
		}

		PowersMod.scheduleDelayed(level.getServer(), 25, () -> {
			if (!player.isAlive()) return;
			Vec3 follow = player.position().add(0, 1, 0);
			PowerFx.ring(level, follow.add(0, -0.3, 0), 6.0, rgb, 40, phase + 1.3);
			PowerFx.coloredBurst(level, follow, rgb, 32, 1.2);
			PowerFx.burst(level, follow, ParticleTypes.ELECTRIC_SPARK, 18, 0.9, 0.12);
			PowerFx.sound(level, follow, SoundEvents.BEACON_DEACTIVATE, 1.0f, 0.6f);
		});
	}

	/** A cold, dragging rejection — used when the dark realm refuses entry. */
	public static void voidReject(ServerLevel level, ServerPlayer player) {
		Vec3 pos = player.position().add(0, 1, 0);
		PowerFx.burst(level, pos, ParticleTypes.REVERSE_PORTAL, 22, 0.9, 0.05);
		PowerFx.spiral(level, pos, 0.9, 2.8, 0x2E0854, 22, 0.0);
		PowerFx.coloredBurst(level, pos, 0x4A235A, 18, 0.6);
		PowerFx.sound(level, pos, SoundEvents.WITHER_AMBIENT, 0.9f, 0.5f);
	}

	/** Crimson chain-flash when a dimensional anchor forbids travel. */
	public static void chainBlock(ServerLevel level, ServerPlayer player) {
		Vec3 pos = player.position().add(0, 1, 0);
		PowerFx.burst(level, pos, ParticleTypes.ELECTRIC_SPARK, 14, 0.5, 0.06);
		PowerFx.coloredBurst(level, pos, 0xFF4D4D, 18, 0.6);
		PowerFx.ring(level, pos.add(0, -0.3, 0), 2.2, 0xFF4D4D, 18, 0.0);
		PowerFx.sound(level, pos, SoundEvents.BEACON_DEACTIVATE, 0.7f, 0.9f);
	}

	/** A shimmering wall-flash when a dimension outright refuses entry. */
	public static void barrier(ServerLevel level, ServerPlayer player, int rgb) {
		Vec3 pos = player.position().add(0, 1, 0);
		PowerFx.ring(level, pos.add(0, -0.3, 0), 2.5, rgb, 24, 0.0);
		PowerFx.burst(level, pos, ParticleTypes.END_ROD, 16, 0.6, 0.25);
		PowerFx.coloredBurst(level, pos, rgb, 20, 0.8);
		PowerFx.sound(level, pos, SoundEvents.BEACON_DEACTIVATE, 0.8f, 1.1f);
	}
}

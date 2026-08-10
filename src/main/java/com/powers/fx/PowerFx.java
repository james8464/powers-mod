package com.powers.fx;

import com.powers.PowersParticles;
import com.powers.PowersSounds;
import com.powers.config.PowersConfigLoader;
import com.powers.diagnostics.ServerRuntimeMetrics;
import com.powers.power.abilities.VoidBeamRules;
import com.powers.network.MagicFxPackets;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
	private static final int MAX_VIEWER_PARTICLES_PER_TICK = 128;
	private static final double MAX_PARTICLE_RANGE = 128.0;
	private static final java.util.Map<MinecraftServer, ViewerParticleBudget> BUDGETS =
			new java.util.WeakHashMap<>();

	private PowerFx() {
	}

	/** puffs a cloud of particles around a point */
	public static void burst(ServerLevel level, Vec3 pos, ParticleOptions particle, int count, double spread, double speed) {
		sendPerViewer(level, pos, particle, count, spread, speed, false);
	}

	/**
	 * Sends dense scatter per viewer so a nearby first-person camera receives a
	 * quarter-density core while observers outside four blocks retain the full silhouette.
	 */
	public static void clarityBurst(ServerLevel level, Vec3 pos, ParticleOptions particle,
			int count, double spread, double speed) {
		sendPerViewer(level, pos, particle, count, spread, speed, true);
	}

	private static void sendPerViewer(ServerLevel level, Vec3 pos, ParticleOptions particle,
			int count, double spread, double speed, boolean protectFirstPersonClarity) {
		if (count <= 0) return;
		ViewerParticleBudget budget = budget(level);
		long tick = level.getServer().getTickCount();
		for (ServerPlayer viewer : level.players()) {
			double distanceSquared = viewer.getEyePosition().distanceToSqr(pos);
			int requested = protectFirstPersonClarity
					? ParticleBudget.viewerCount(count, distanceSquared) : count;
			int granted = budget.claim(tick, viewer.getUUID(), requested, distanceSquared);
			if (granted <= 0) continue;
			ServerRuntimeMetrics.recordParticles(level.getServer(), tick, granted);
			level.sendParticles(viewer, particle, false, false,
					pos.x, pos.y, pos.z, granted, spread, spread, spread, speed);
		}
	}

	private static ViewerParticleBudget budget(ServerLevel level) {
		int limit = PowersConfigLoader.get().maxParticlesPerTick();
		MinecraftServer server = level.getServer();
		int viewerLimit = Math.min(MAX_VIEWER_PARTICLES_PER_TICK, Math.max(1, limit));
		ViewerParticleBudget budget = BUDGETS.get(server);
		if (budget == null || budget.serverLimit() != limit || budget.viewerLimit() != viewerLimit) {
			budget = new ViewerParticleBudget(limit, viewerLimit, MAX_PARTICLE_RANGE);
			BUDGETS.put(server, budget);
		}
		return budget;
	}

	/** Creates a colourable magic particle without Minecraft's potion-effect cloud. */
	public static DustParticleOptions dust(int rgb, float scale) {
		return new DustParticleOptions(rgb & 0xFFFFFF, Math.clamp(scale, 0.01F, 4.0F));
	}

	/** Puffs a restrained dust cloud tinted with an RGB colour. */
	public static void coloredBurst(ServerLevel level, Vec3 pos, int rgb, int count, double spread) {
		burst(level, pos, dust(rgb, 1.0F), count, spread, 0.0);
	}

	/** draws a straight line of particles between two points */
	public static void beam(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle, int steps) {
		if (steps <= 0 || !finite(from) || !finite(to)) return;
		int requested = Math.min(64, steps);
		Vec3 midpoint = from.add(to).scale(0.5);
		ViewerParticleBudget budget = budget(level);
		long tick = level.getServer().getTickCount();
		BeamFxStyle style = BeamFxStyle.from(particle);
		int color = BeamFxStyle.color(particle);
		long eventId = Integer.toUnsignedLong(java.util.Objects.hash(tick, from, to, style, color));
		for (ServerPlayer viewer : level.players()) {
			double distanceSquared = viewer.getEyePosition().distanceToSqr(midpoint);
			int visible = ParticleBudget.viewerCount(requested, distanceSquared);
			int granted = budget.claim(tick, viewer.getUUID(), visible, distanceSquared);
			if (granted <= 0) continue;
			ServerRuntimeMetrics.recordParticles(level.getServer(), tick, granted);
			MagicFxPackets.sendBeam(viewer, new MagicFxPackets.BeamFxPayload(eventId, style,
					from.x, from.y, from.z, to.x, to.y, to.z, granted, color));
		}
	}

	private static boolean finite(Vec3 point) {
		return Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
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
		beam(level, from, midpoint, dust(attacker, 0.85F), 8);
		beam(level, to, midpoint, dust(defender, 0.85F), 8);
		burst(level, midpoint, ParticleTypes.ELECTRIC_SPARK, 16, 0.4, 0.08);
		sound(level, midpoint, SoundEvents.BEACON_DEACTIVATE, 0.8f, 1.4f);
	}

	/** Releases a server-authoritative kinetic dash with a readable direction. */
	public static void speedBurstRelease(ServerLevel level, Vec3 center, Vec3 movement,
			boolean followUp) {
		int primary = followUp ? 0xFFD166 : 0xD7F8FF;
		int secondary = followUp ? 0xD7F8FF : 0x7DEBFF;
		burst(level, center, ParticleTypes.ELECTRIC_SPARK, followUp ? 22 : 16, 0.48, 0.24);
		burst(level, center, ParticleTypes.CLOUD, followUp ? 18 : 12, 0.38, 0.28);
		burst(level, center, PowersParticles.SPARK, followUp ? 12 : 8, 0.34, 0.16);
		coloredBurst(level, center, primary, followUp ? 18 : 12, 0.42);
		Vec3 end = center.add(movement.scale(1.4));
		beam(level, center, end, dust(secondary, followUp ? 1.15F : 0.9F), followUp ? 16 : 12);
		rune(level, center.add(0.0, -0.42, 0.0), followUp ? 1.35 : 1.05,
				primary, followUp ? 24 : 18, followUp ? Math.PI : 0.0);
		sound(level, center, SoundEvents.FIREWORK_ROCKET_SHOOT, followUp ? 1.4F : 1.0F,
				followUp ? 1.75F : 1.48F);
		sound(level, center, SoundEvents.BREEZE_SHOOT, followUp ? 1.2F : 0.75F,
				followUp ? 0.82F : 1.05F);
	}

	/** Draws one bounded afterimage ribbon between observed server positions. */
	public static void speedBurstWake(ServerLevel level, Vec3 from, Vec3 to,
			boolean followUp, int age) {
		int color = followUp ? 0xFFD166 : 0xA9F4FF;
		if (from.distanceToSqr(to) > 1.0E-4) {
			beam(level, from.add(0.0, 0.45, 0.0), to.add(0.0, 0.45, 0.0),
					PowersParticles.RIBBON, followUp ? 10 : 7);
		}
		burst(level, from.add(0.0, 0.35, 0.0), ParticleTypes.CLOUD, 3, 0.18, 0.06);
		burst(level, to.add(0.0, 0.45, 0.0), PowersParticles.SPARK,
				followUp ? 4 : 2, 0.22, 0.08);
		coloredBurst(level, to.add(0.0, 0.45, 0.0), color, followUp ? 4 : 2, 0.2);
		if ((age & 1) == 0) {
			ring(level, from.add(0.0, 0.25, 0.0), followUp ? 0.72 : 0.55,
					color, followUp ? 12 : 8, age * 0.35);
		}
	}

	/** Announces the short Motion-rank window for one paid follow-up dash. */
	public static void secondStepReady(ServerLevel level, Vec3 center) {
		rune(level, center.add(0.0, -0.42, 0.0), 1.2, 0xD7F8FF, 20, 0.0);
		rune(level, center.add(0.0, -0.34, 0.0), 0.82, 0xFFD166, 16, Math.PI);
		burst(level, center, dust(0xD7F8FF, 0.85F), 14, 0.44, 0.03);
		burst(level, center, PowersParticles.MOTE, 10, 0.36, 0.05);
		sound(level, center, SoundEvents.AMETHYST_BLOCK_CHIME, 0.85F, 1.72F);
		sound(level, center, SoundEvents.BEACON_POWER_SELECT, 0.65F, 1.28F);
	}

	/** Detonates the dash endpoint without creating terrain damage. */
	public static void speedBurstImpact(ServerLevel level, Vec3 center, boolean followUp) {
		int primary = followUp ? 0xFFD166 : 0xD7F8FF;
		burst(level, center, ParticleTypes.EXPLOSION, followUp ? 5 : 3, 0.5, 0.08);
		burst(level, center, ParticleTypes.ELECTRIC_SPARK, followUp ? 28 : 20, 1.15, 0.28);
		burst(level, center, PowersParticles.FRACTURE, followUp ? 18 : 12, 1.0, 0.16);
		coloredBurst(level, center, primary, followUp ? 24 : 16, 1.1);
		ring(level, center, followUp ? 3.0 : 2.6, primary, followUp ? 36 : 28, 0.0);
		ring(level, center.add(0.0, 0.12, 0.0), followUp ? 2.35 : 2.0,
				followUp ? 0xD7F8FF : 0x7DEBFF, followUp ? 30 : 22, Math.PI / 12.0);
		sound(level, center, SoundEvents.WARDEN_SONIC_BOOM, followUp ? 1.4F : 0.9F,
				followUp ? 1.35F : 1.62F);
		sound(level, center, SoundEvents.GENERIC_EXPLODE.value(), followUp ? 1.2F : 0.8F,
				followUp ? 1.28F : 1.55F);
	}

	/** Contracts an eclipse seal around a caster during the server-owned charge. */
	public static void voidBeamCharge(ServerLevel level, Vec3 center, int remainingTicks,
			boolean ancientMastery) {
		int bounded = Math.max(0, Math.min(VoidBeamRules.CHARGE_TICKS, remainingTicks));
		int elapsed = VoidBeamRules.CHARGE_TICKS - bounded;
		double radius = 0.38 + bounded * 0.055;
		double phase = level.getGameTime() * 0.24;
		rune(level, center, radius, 0x16051F, ancientMastery ? 16 : 12, phase);
		ring(level, center.add(0.0, 0.08, 0.0), radius * 0.72,
				0x7846B8, ancientMastery ? 14 : 10, -phase * 1.35);
		burst(level, center, PowersParticles.ECLIPSE, ancientMastery ? 5 : 3, radius * 0.35, 0.025);
		if (elapsed == 0 || elapsed == 4 || elapsed == 8 || bounded == 1) {
			sound(level, center, PowersSounds.DARK_WHISPER, 0.45F + elapsed * 0.035F,
					0.62F + elapsed * 0.055F);
			sound(level, center, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.35F,
					0.72F + elapsed * 0.045F);
		}
	}

	/** Tears one layered, bounded ray between authoritative release endpoints. */
	public static void voidBeamRelease(ServerLevel level, Vec3 from, Vec3 to,
			boolean empoweredImpact, boolean ancientMastery) {
		int steps = ancientMastery ? 38 : empoweredImpact ? 34 : 28;
		beam(level, from, to, PowersParticles.RIBBON, steps);
		beam(level, from, to, PowersParticles.ECLIPSE, Math.max(16, steps - 8));
		beam(level, from, to, dust(0x6D32A8, ancientMastery ? 1.35F : 1.0F),
				Math.max(12, steps - 12));
		burst(level, from, ParticleTypes.REVERSE_PORTAL, ancientMastery ? 22 : 14, 0.42, 0.12);
		burst(level, to, PowersParticles.FRACTURE, empoweredImpact ? 26 : 18, 0.7, 0.16);
		ring(level, to, empoweredImpact ? 1.6 : 1.25, 0x2A0C3D,
				empoweredImpact ? 28 : 22, level.getGameTime() * 0.18);
		sound(level, from, PowersSounds.DARK_WHISPER, ancientMastery ? 1.3F : 0.9F,
				ancientMastery ? 0.48F : 0.62F);
		sound(level, to, SoundEvents.WARDEN_SONIC_BOOM, empoweredImpact ? 1.3F : 0.9F,
				empoweredImpact ? 0.58F : 0.72F);
	}

	/** Marks each permitted body bored through by the finite ray. */
	public static void voidBeamPenetration(ServerLevel level, Vec3 point, int index,
			boolean darkResurgence) {
		int count = Math.max(0, Math.min(VoidBeamRules.MAX_PENETRATIONS - 1, index));
		burst(level, point, PowersParticles.FRACTURE, 7 + count * 2, 0.34, 0.09);
		burst(level, point, ParticleTypes.SOUL, darkResurgence ? 8 : 5, 0.28, 0.07);
		ring(level, point, 0.48 + count * 0.08, darkResurgence ? 0x7C36C8 : 0x2A0C3D,
				10 + count * 2, count * Math.PI / 5.0);
	}

	/** Gives each hard counter a different geometry, colour pair, and sound. */
	public static void voidBeamCountered(ServerLevel level, Vec3 point,
			VoidBeamRules.Counterplay counterplay) {
		if (counterplay == null || counterplay == VoidBeamRules.Counterplay.NONE) return;
		int primary = switch (counterplay) {
			case LIGHT -> 0xFFF4C7;
			case AMETHYST -> 0xB36BFF;
			case SANCTUARY -> 0x8CFF98;
			case KINETIC_WARD, FORCEFIELD, SAFE_ZONE -> 0x70D6FF;
			case NONE -> 0x2A0C3D;
		};
		int secondary = switch (counterplay) {
			case LIGHT -> 0xFFFFFF;
			case AMETHYST -> 0x5E2A84;
			case SANCTUARY -> 0xFFE8A3;
			case KINETIC_WARD, FORCEFIELD, SAFE_ZONE -> 0xD6F5FF;
			case NONE -> 0x7846B8;
		};
		rune(level, point, 1.35, primary, 24, level.getGameTime() * 0.12);
		ring(level, point.add(0.0, 0.12, 0.0), 0.82, secondary, 18,
				-level.getGameTime() * 0.18);
		burst(level, point, counterplay == VoidBeamRules.Counterplay.AMETHYST
				? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD, 18, 0.55, 0.11);
		burst(level, point, PowersParticles.FRACTURE, 14, 0.48, 0.08);
		sound(level, point, counterplay == VoidBeamRules.Counterplay.LIGHT
				? PowersSounds.LIGHT_CHORUS
				: counterplay == VoidBeamRules.Counterplay.AMETHYST
						? PowersSounds.AMETHYST_FRACTURE : PowersSounds.WARD_IMPACT,
				1.05F, counterplay == VoidBeamRules.Counterplay.LIGHT ? 1.35F : 0.82F);
	}

	/** Opens and sustains a terrain-safe abyssal scar on its five-tick beat. */
	public static void voidScarPulse(ServerLevel level, Vec3 center, double radius,
			int age, boolean ancientMastery) {
		double phase = age * 0.17;
		ring(level, center, radius, 0x16051F, ancientMastery ? 30 : 24, phase);
		ring(level, center.add(0.0, 0.1, 0.0), radius * 0.68, 0x7846B8,
				ancientMastery ? 24 : 18, -phase * 1.45);
		spiral(level, center.add(0.0, -0.55, 0.0), radius * 0.42, 1.1,
				0x4C1D73, ancientMastery ? 18 : 13, -phase);
		burst(level, center, PowersParticles.ECLIPSE, ancientMastery ? 9 : 6,
				radius * 0.35, 0.035);
		if (age % 20 == 0) sound(level, center, PowersSounds.DARK_WHISPER, 0.55F, 0.52F);
	}

	/** Collapses an expired or unloaded scar inward without touching terrain. */
	public static void voidScarCollapse(ServerLevel level, Vec3 center, double radius) {
		burst(level, center, ParticleTypes.REVERSE_PORTAL, 28, radius * 0.48, 0.18);
		burst(level, center, PowersParticles.FRACTURE, 18, radius * 0.4, 0.09);
		rune(level, center, Math.max(0.7, radius * 0.6), 0x7846B8, 20, Math.PI / 8.0);
		sound(level, center, PowersSounds.RIFT_CLOSE, 0.9F, 0.58F);
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
				dust(0x8FE9FF, 1.0F), 16);
		burst(level, center, dust(0x8FE9FF, 0.9F), 14, 0.45, 0.02);
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

	/** Announces a rare rank completion with a layered, allegiance-coloured rite. */
	public static void rankAwakening(ServerPlayer player, boolean darkness, int level) {
		ServerLevel world = player.level();
		Vec3 feet = player.position().add(0.0, 0.08, 0.0);
		int primary = darkness ? 0x4A174F : 0xFFF0A6;
		int secondary = darkness ? 0xA04FC7 : 0x77E8FF;
		double radius = 1.5 + Math.min(level, 10) * 0.08;
		rune(world, feet, radius, primary, 28, level * 0.17);
		rune(world, feet.add(0.0, 1.0, 0.0), radius * 0.68, secondary, 20, -level * 0.13);
		spiral(world, feet, radius * 0.7, 2.5, secondary, 36, level * 0.2);
		burst(world, player.getEyePosition(), darkness ? ParticleTypes.SOUL_FIRE_FLAME
				: ParticleTypes.END_ROD, 18, 0.65, 0.06);
		sound(world, feet, PowersSounds.RANK_AWAKEN, 1.5F, darkness ? 0.72F : 1.18F);
	}

	/** a cycling rainbow rgb color, for rainbow steve's effects */
	public static int rainbow(int tick, int step) {
		return FxColorMath.rainbow(tick, step);
	}

	public static void clearBudgets() {
		BUDGETS.clear();
	}
}

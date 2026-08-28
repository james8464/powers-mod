package com.powers.boss;

import com.powers.PowerStatusEffects;
import com.powers.PowersSounds;
import com.powers.animation.CastingPoseMapping;
import com.powers.animation.CastingPoseService;
import com.powers.boss.FirstVesselPowerAction.Kind;
import com.powers.entity.FirstVessel;
import com.powers.fx.PowerFx;
import com.powers.power.AmethystDampening;
import com.powers.power.abilities.ThunderclapRules;
import com.powers.power.PowerDamage;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/** Entity-safe, terrain-safe combat adapters for the complete innate catalogue. */
public final class FirstVesselCombat {
	private FirstVesselCombat() {
	}

	public static void cast(ServerLevel level, FirstVessel boss, LivingEntity target,
			FirstVesselPowerAction action, FirstVesselPhase phase) {
		if (!target.isAlive()) return;
		switch (action.powerId()) {
			case "size_shift" -> ward(level, boss, 1.35F, 100);
			case "time_shift", "astral_projection" -> step(level, boss, target, 5.0);
			case "flight", "speed_burst", "super_speed" -> rush(level, boss, target);
			case "starfall" -> area(level, boss, target.position(), 10.0, 72.0F, true);
			case "void_beam" -> beam(level, boss, target, 82.0F, 0x6D32A8);
			case "fireball" -> projectile(level, boss, target, 62.0F, 0x8A1F14);
			case "lightning_strike" -> lightning(level, boss, target);
			case "thunderclap" -> thunderclap(level, boss);
			case "telekinesis", "breezy_bash" -> throwTarget(level, boss, target);
			case "energy_beam" -> beam(level, boss, target, 76.0F, 0xFF6A20);
			case "plant_healing_acceleration" -> recover(level, boss, 260.0F);
			case "invisibility" -> veil(level, boss);
			case "time_freeze" -> freeze(level, boss, target);
			case "forcefield", "double_health" -> ward(level, boss, 1.0F, 180);
			case "gravity_displacement", "vessel_possession" -> crush(level, boss, target);
			case "energy_drain" -> drain(level, boss, target);
			case "ice_manipulation" -> iceLance(level, boss, target);
			default -> fallback(level, boss, target, action.kind());
		}
		if (phase == FirstVesselPhase.LAST_COVENANT) {
			PowerFx.ring(level, boss.position().add(0.0, 0.08, 0.0), 2.2,
					0x9A4FB4, 18, level.getGameTime() * 0.13);
		}
		boolean targetPresentation = switch (action.kind()) {
			case PROJECTILE, BEAM, AREA, CONTROL -> !PowerProtection.isSafeZone(level, target.position())
					&& !SpellFieldManager.isSanctuaryProtected(level, target)
					&& !AmethystDampening.isDampened(target);
			case MOBILITY, DEFENSE, RECOVERY -> true;
		};
		if (targetPresentation) {
			CastingPoseService.start(boss, CastingPoseMapping.forFirstVessel(action.kind()),
					CastingPoseMapping.style(boss), CastingPoseMapping.hand(action.powerId()),
					CastingPoseMapping.duration(action.kind()));
		}
	}

	/** Unique phase technique: pull every bounded opponent into one collapsing seal. */
	public static void worldSuture(ServerLevel level, FirstVessel boss) {
		Vec3 center = boss.position();
		for (Projectile projectile : BoundedEntityCandidates.ofClass(level, Projectile.class,
				AABB.ofSize(center, 48.0, 48.0, 48.0), FirstVesselRules.MAX_CANDIDATES,
				projectile -> projectile.isAlive() && projectile.getOwner() != boss
						&& !PowerProtection.isSafeZone(level, projectile.position()))) {
			projectile.discard();
			PowerFx.burst(level, projectile.position(), com.powers.PowersParticles.ECLIPSE,
					3, 0.2, 0.03);
		}
		for (LivingEntity target : candidates(level, boss, center, 24.0)) {
			if (!mayControl(level, target)) continue;
			Vec3 pull = center.subtract(target.position());
			if (pull.lengthSqr() > 1.0E-6) target.setDeltaMovement(
					target.getDeltaMovement().scale(0.25).add(pull.normalize().scale(1.15)));
			harm(level, boss, target, 42.0F);
		}
		PowerFx.rune(level, center, 8.0, 0x51205F, 56, level.getGameTime() * 0.11);
		PowerFx.spiral(level, center, 6.5, 9.0, 0xB26CD0, 48, 0.0);
		PowerFx.sound(level, center, PowersSounds.DARK_WHISPER, 2.2F, 0.42F);
	}

	/** Unique last-phase arena strike; enormous presentation, bounded living damage, no terrain. */
	public static void lastFirmament(ServerLevel level, FirstVessel boss) {
		Vec3 center = boss.position();
		for (LivingEntity target : candidates(level, boss, center, 32.0)) {
			if (!harm(level, boss, target, Math.min(180.0F, target.getMaxHealth() * 0.32F))) continue;
			if (!mayControl(level, target)) continue;
			Vec3 away = target.position().subtract(center);
			if (away.lengthSqr() > 1.0E-6) target.setDeltaMovement(away.normalize().scale(2.2).add(0, 1.1, 0));
		}
		PowerFx.burst(level, center, ParticleTypes.EXPLOSION_EMITTER, 4, 2.0, 0.0);
		PowerFx.rune(level, center, 14.0, 0xECE2FF, 64, 0.0);
		PowerFx.rune(level, center.add(0, 0.2, 0), 10.0, 0x19031F, 56, Math.PI / 16.0);
		PowerFx.sound(level, center, SoundEvents.END_PORTAL_SPAWN, 4.0F, 0.45F);
		PowerFx.sound(level, center, SoundEvents.GENERIC_EXPLODE.value(), 4.0F, 0.6F);
	}

	private static void lightning(ServerLevel level, FirstVessel boss, LivingEntity target) {
		var bolt = net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT.create(level,
				net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.setVisualOnly(true);
			bolt.setPos(target.position());
			level.addFreshEntity(bolt);
		}
		harm(level, boss, target, 70.0F);
		PowerFx.beam(level, target.position().add(0, 18, 0), target.getEyePosition(),
				ParticleTypes.ELECTRIC_SPARK, 22);
	}

	private static void projectile(ServerLevel level, FirstVessel boss, LivingEntity target,
			float damage, int color) {
		harm(level, boss, target, damage);
		PowerFx.beam(level, boss.getEyePosition(), target.getEyePosition(),
				ParticleTypes.SOUL_FIRE_FLAME, 24);
		PowerFx.rune(level, target.position(), 2.4, color, 24, 0.0);
		PowerFx.sound(level, target.position(), SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 0.72F);
	}

	private static void beam(ServerLevel level, FirstVessel boss, LivingEntity target,
			float damage, int color) {
		harm(level, boss, target, damage);
		PowerFx.beam(level, boss.getEyePosition(), target.getEyePosition(),
				com.powers.PowersParticles.GLYPH, 34);
		PowerFx.rune(level, target.position(), 1.7, color, 26, 0.0);
		PowerFx.sound(level, boss.position(), SoundEvents.WARDEN_SONIC_BOOM, 1.4F, 0.62F);
	}

	private static void area(ServerLevel level, FirstVessel boss, Vec3 center,
			double radius, float damage, boolean vertical) {
		for (LivingEntity target : candidates(level, boss, center, radius)) {
			if (!harm(level, boss, target, damage) || !mayControl(level, target)) continue;
			Vec3 push = target.position().subtract(center);
			if (push.lengthSqr() > 1.0E-6) target.setDeltaMovement(push.normalize().scale(1.1)
					.add(0.0, vertical ? 1.3 : 0.55, 0.0));
		}
		PowerFx.rune(level, center, radius, 0x7C3E92, 44, 0.0);
		PowerFx.burst(level, center, ParticleTypes.EXPLOSION, 5, 1.5, 0.06);
	}

	private static void thunderclap(ServerLevel level, FirstVessel boss) {
		area(level, boss, boss.position(), 12.0, 58.0F, false);
		PowerFx.sound(level, boss.position(), SoundEvents.WARDEN_SONIC_BOOM, 2.0F, 1.4F);
	}

	private static void throwTarget(ServerLevel level, FirstVessel boss, LivingEntity target) {
		if (!mayControl(level, target)) return;
		Vec3 away = target.position().subtract(boss.position()).normalize();
		target.setDeltaMovement(away.scale(2.0).add(0.0, 1.4, 0.0));
		harm(level, boss, target, 46.0F);
		PowerFx.spiral(level, target.position(), 1.2, 4.0, 0xB481D1, 24, 0.0);
	}

	private static void crush(ServerLevel level, FirstVessel boss, LivingEntity target) {
		if (!mayControl(level, target)) return;
		target.setDeltaMovement(Vec3.ZERO);
		target.addEffect(PowerStatusEffects.hidden(MobEffects.SLOWNESS, 80, 6, false, true));
		harm(level, boss, target, 68.0F);
		PowerFx.rune(level, target.position(), 2.8, 0x431252, 32, 0.0);
	}

	private static void freeze(ServerLevel level, FirstVessel boss, LivingEntity target) {
		for (LivingEntity affected : candidates(level, boss, target.position(), 10.0)) {
			if (!mayControl(level, affected)) continue;
			affected.addEffect(PowerStatusEffects.hidden(MobEffects.SLOWNESS, 80, 8, false, true));
			affected.addEffect(PowerStatusEffects.hidden(MobEffects.MINING_FATIGUE, 80, 4, false, true));
		}
		PowerFx.rune(level, target.position(), 10.0, 0xA8F4FF, 48, 0.0);
	}

	private static void drain(ServerLevel level, FirstVessel boss, LivingEntity target) {
		if (harm(level, boss, target, 56.0F)) recover(level, boss, 160.0F);
		PowerFx.beam(level, target.getEyePosition(), boss.getEyePosition(),
				com.powers.PowersParticles.ECLIPSE, 28);
	}

	private static void iceLance(ServerLevel level, FirstVessel boss, LivingEntity target) {
		if (harm(level, boss, target, 60.0F) && mayControl(level, target)) {
			target.addEffect(PowerStatusEffects.hidden(MobEffects.SLOWNESS, 120, 3, false, true));
		}
		PowerFx.beam(level, boss.getEyePosition(), target.getEyePosition(),
				ParticleTypes.SNOWFLAKE, 26);
	}

	private static void recover(ServerLevel level, FirstVessel boss, float amount) {
		boss.heal(amount);
		PowerFx.spiral(level, boss.position(), 2.0, 5.5, 0x8B4E9F, 30, 0.0);
		PowerFx.sound(level, boss.position(), SoundEvents.BEACON_POWER_SELECT, 1.0F, 0.55F);
	}

	private static void ward(ServerLevel level, FirstVessel boss, float scale, int absorptionTicks) {
		boss.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(scale);
		boss.addEffect(PowerStatusEffects.hidden(MobEffects.ABSORPTION, absorptionTicks, 5, false, true));
		boss.addEffect(PowerStatusEffects.hidden(MobEffects.RESISTANCE, absorptionTicks, 2, false, true));
		PowerFx.rune(level, boss.position(), 3.0, 0xB979D0, 32, 0.0);
	}

	private static void veil(ServerLevel level, FirstVessel boss) {
		boss.addEffect(PowerStatusEffects.hidden(MobEffects.INVISIBILITY, 100, 0, false, true));
		PowerFx.burst(level, boss.position(), com.powers.PowersParticles.ECLIPSE, 18, 0.7, 0.06);
	}

	private static void rush(ServerLevel level, FirstVessel boss, LivingEntity target) {
		Vec3 direction = target.position().subtract(boss.position());
		if (direction.lengthSqr() > 1.0E-6) boss.setDeltaMovement(direction.normalize().scale(2.2).add(0, 0.35, 0));
		PowerFx.beam(level, boss.position(), target.position(), com.powers.PowersParticles.RIBBON, 18);
	}

	private static void step(ServerLevel level, FirstVessel boss, LivingEntity target, double radius) {
		Vec3 look = target.getLookAngle();
		ThunderclapRules.HorizontalDirection forward = ThunderclapRules.horizontalDirection(
				look.x, look.z, target.getYRot());
		Vec3 destination = target.position().subtract(
				new Vec3(forward.x(), 0.0, forward.z()).scale(radius));
		if (boss.randomTeleport(destination.x, destination.y, destination.z, true)) {
			PowerFx.spiral(level, destination, 1.2, 3.5, 0x54206B, 22, 0.0);
		}
	}

	private static void fallback(ServerLevel level, FirstVessel boss, LivingEntity target, Kind kind) {
		if (kind == Kind.MOBILITY) rush(level, boss, target);
		else if (kind == Kind.RECOVERY || kind == Kind.DEFENSE) recover(level, boss, 120.0F);
		else projectile(level, boss, target, 48.0F, 0x6B2A79);
	}

	private static boolean harm(ServerLevel level, FirstVessel boss, LivingEntity target, float damage) {
		if (PowerProtection.isSafeZone(level, target.position())
				|| SpellFieldManager.isSanctuaryProtected(level, target)) {
			PowerFx.cancelled(level, target.getEyePosition(), 0xE7D7FF);
			return false;
		}
		float adjusted = AmethystDampening.isDampened(target) ? damage * 0.35F : damage;
		float capped = Math.min(adjusted, Math.max(24.0F, target.getMaxHealth() * 0.32F));
		return target.hurtServer(level, PowerDamage.source(boss), capped);
	}

	private static boolean mayControl(ServerLevel level, LivingEntity target) {
		return FirstVesselRules.mayControl(PowerProtection.isSafeZone(level, target.position()),
				SpellFieldManager.isSanctuaryProtected(level, target),
				AmethystDampening.isDampened(target));
	}

	private static java.util.List<LivingEntity> candidates(ServerLevel level,
			FirstVessel boss, Vec3 center, double radius) {
		return BoundedEntityCandidates.living(level, AABB.ofSize(center,
				radius * 2.0, radius * 2.0, radius * 2.0), FirstVesselRules.MAX_CANDIDATES,
				candidate -> candidate != boss && !(candidate instanceof FirstVessel)
						&& candidate.isAlive() && candidate.distanceToSqr(center) <= radius * radius,
				Comparator.comparingDouble(candidate -> candidate.distanceToSqr(center)));
	}
}

package com.powers.power.artifact;

import com.powers.PowerStatusEffects;
import com.powers.fx.PowerFx;
import com.powers.item.artifact.ArtifactActionDefinition;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.player.SkillSystem;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.power.PowerTargeting;
import com.powers.power.travel.SafeDestinationResolver;
import com.powers.power.travel.TravelKind;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.BoundedEntityCandidates;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** Server-authoritative implementations for the eleven opposed dominion rites. */
public final class ArtifactWorldActions {
	private static final double TARGET_RANGE = 96.0;

	private ArtifactWorldActions() {
	}

	public static boolean activate(ServerPlayer player, ArtifactActionDefinition action) {
		return switch (action.abilityId()) {
			case "call_hollowed", "call_radiant" -> ArtifactGuardianSummons.summon(
					player, action.alignment(), 4, false, null, true) > 0;
			case "blight_ground", "consecrate_ground" -> ground(player, action.alignment(), false);
			case "umbral_step", "dawnstride" -> step(player, action.alignment());
			case "night_chain", "covenant_chain" -> chain(player, action.alignment());
			case "eclipse_wave", "daybreak_wave" -> wave(player, action.alignment());
			case "abyss_gate", "heaven_gate" -> ArtifactGateManager.open(player, action.alignment());
			case "devour_light", "banish_darkness" -> ground(player, action.alignment(), true);
			case "black_decree", "divine_decree" -> decree(player, action.alignment());
			case "event_horizon", "solar_firmament" -> field(player, action.alignment());
			case "deathless_night", "second_dawn" -> ArtifactDeathWardManager.arm(player, action.alignment());
			case "legion_eclipse", "host_heaven" -> host(player, action.alignment());
			default -> false;
		};
	}

	private static boolean ground(ServerPlayer player, ArtifactAlignment alignment, boolean opposedOnly) {
		int queued = ArtifactGroundWorkQueue.enqueueDisc(player, alignment,
				opposedOnly ? 9 : 6, opposedOnly, opposedOnly);
		if (queued <= 0) return false;
		ServerLevel level = (ServerLevel) player.level();
		int color = alignment == ArtifactAlignment.DARKNESS ? 0x3A0B52 : 0xFFF2B2;
		PowerFx.rune(level, player.position(), opposedOnly ? 9.0 : 6.0,
				color, opposedOnly ? 56 : 40, 0.0);
		PowerFx.spiral(level, player.position(), 1.5, 4.0, color, 30, Math.PI / 8.0);
		PowerFx.sound(level, player.position(), alignment == ArtifactAlignment.DARKNESS
				? SoundEvents.RESPAWN_ANCHOR_CHARGE : SoundEvents.BEACON_POWER_SELECT,
				1.2F, alignment == ArtifactAlignment.DARKNESS ? 0.6F : 1.45F);
		return true;
	}

	private static boolean step(ServerPlayer player, ArtifactAlignment alignment) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 origin = player.position();
		Vec3 look = player.getLookAngle();
		Vec3 destination = null;
		for (int distance = 24; distance >= 3; distance--) {
			Vec3 candidate = origin.add(look.scale(distance));
			if (!LoadedChunks.contains(level, BlockPos.containing(candidate))) continue;
			if (SafeDestinationResolver.validate(player, level, candidate, TravelKind.POWER).allowed()) {
				destination = candidate;
				break;
			}
		}
		if (destination == null) return false;
		int color = alignment == ArtifactAlignment.DARKNESS ? 0x48105D : 0xFFF2B2;
		PowerFx.beam(level, origin.add(0.0, 1.0, 0.0), destination.add(0.0, 1.0, 0.0),
				alignment == ArtifactAlignment.DARKNESS ? ParticleTypes.REVERSE_PORTAL
						: ParticleTypes.END_ROD, 32);
		player.teleport(new TeleportTransition(level, destination, player.getDeltaMovement(),
				player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
		PowerFx.rune(level, origin, 1.4, color, 24, 0.0);
		PowerFx.rune(level, destination, 1.4, color, 24, Math.PI);
		return true;
	}

	private static boolean chain(ServerPlayer player, ArtifactAlignment alignment) {
		LivingEntity target = PowerTargeting.findLivingTarget(player, 64.0);
		if (!eligible(player, target)) return false;
		ServerLevel level = (ServerLevel) player.level();
		boolean ally = player.isAlliedTo(target);
		if (alignment == ArtifactAlignment.LIGHT && ally) {
			if (!ArtifactCovenantManager.link(player, target, 600)) return false;
			target.addEffect(PowerStatusEffects.hidden(MobEffects.REGENERATION, 600, 2, false, true));
			target.addEffect(PowerStatusEffects.hidden(MobEffects.ABSORPTION, 600, 3, false, true));
		} else {
			if (!PowerProtection.mayForceMove(player, target) || !PowerProtection.mayHarm(player, target)) return false;
			if (!ArtifactChainManager.bind(player, target, alignment)) return false;
		}
		PowerFx.beam(level, player.getEyePosition(), target.getEyePosition(),
				alignment == ArtifactAlignment.DARKNESS ? ParticleTypes.SOUL_FIRE_FLAME
						: ParticleTypes.END_ROD, 36);
		PowerFx.rune(level, target.position(), 2.2,
				alignment == ArtifactAlignment.DARKNESS ? 0x48105D : 0xFFF2B2, 32, 0.0);
		return true;
	}

	private static boolean wave(ServerPlayer player, ArtifactAlignment alignment) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 origin = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		AABB bounds = player.getBoundingBox().inflate(32.0);
		List<LivingEntity> targets = BoundedEntityCandidates.living(level, bounds, 192,
				target -> target != player && target.isAlive() && inCone(origin, look, target.getEyePosition()),
				Comparator.comparingDouble(player::distanceToSqr));
		for (LivingEntity target : targets) {
			boolean dark = target.entityTags().contains(SkillSystem.DARKNESS_TAG);
			boolean hostile = alignment == ArtifactAlignment.DARKNESS ? !dark : dark;
			if (alignment == ArtifactAlignment.LIGHT && !hostile) {
				target.heal(player.isAlliedTo(target) ? 16.0F : 5.0F);
				for (MobEffectInstance effect : List.copyOf(target.getActiveEffects())) {
					if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
						target.removeEffect(effect.getEffect());
					}
				}
			} else {
				ArtifactImpactRules.Decision decision = ArtifactImpactRules.decide(hostile,
						AmethystDampening.isDampened(target),
						PowerProtection.mayHarm(player, target)
								&& !SpellFieldManager.isSanctuaryProtected(level, target),
						PowerProtection.mayForceMove(player, target),
						SpellFieldManager.blocksForcedMovement(level, target, player.getUUID()));
				if (decision.damage()) {
					target.hurtServer(level, PowerDamage.source(player),
							alignment == ArtifactAlignment.DARKNESS ? 85.0F : 70.0F);
				}
				if (decision.move()) {
					target.setDeltaMovement(target.getDeltaMovement().add(
							look.x * 3.5, 0.5, look.z * 3.5));
					target.hurtMarked = true;
				}
			}
		}
		List<Projectile> projectiles = BoundedEntityCandidates.collect(level,
				EntityTypeTest.forClass(Projectile.class), bounds, 128,
				projectile -> inCone(origin, look, projectile.position())
						&& !PowerProtection.isSafeZone(level, projectile.position()),
				Comparator.comparingDouble(player::distanceToSqr));
		for (Projectile projectile : projectiles) projectile.discard();
		int color = alignment == ArtifactAlignment.DARKNESS ? 0x3A0B52 : 0xFFFFFF;
		for (int distance = 4; distance <= 32; distance += 4) {
			PowerFx.ring(level, origin.add(look.scale(distance)), distance * 0.22,
					color, 28, distance * 0.12);
		}
		PowerFx.sound(level, origin, SoundEvents.WARDEN_SONIC_BOOM, 2.0F,
				alignment == ArtifactAlignment.DARKNESS ? 0.55F : 1.45F);
		return !targets.isEmpty() || !projectiles.isEmpty();
	}

	private static boolean decree(ServerPlayer player, ArtifactAlignment alignment) {
		LivingEntity target = PowerTargeting.findLivingTarget(player, TARGET_RANGE);
		return eligible(player, target) && ArtifactDecreeManager.mark(player, target, alignment);
	}

	private static boolean field(ServerPlayer player, ArtifactAlignment alignment) {
		Vec3 center = player.getEyePosition().add(player.getLookAngle().scale(12.0));
		return ArtifactFieldManager.start(player, center, alignment);
	}

	private static boolean host(ServerPlayer player, ArtifactAlignment alignment) {
		int spawned = ArtifactGuardianSummons.summon(player, alignment, 2, true, null, true);
		if (spawned == 0) return false;
		ArtifactGroundWorkQueue.enqueueDisc(player, alignment, 10, false, false);
		ArtifactFieldManager.start(player, player.position(), alignment);
		ServerLevel level = (ServerLevel) player.level();
		PowerFx.beam(level, player.position(), player.position().add(0.0, 72.0, 0.0),
				alignment == ArtifactAlignment.DARKNESS ? ParticleTypes.SOUL_FIRE_FLAME
						: ParticleTypes.END_ROD, 64);
		PowerFx.rune(level, player.position(), 14.0,
				alignment == ArtifactAlignment.DARKNESS ? 0x21002E : 0xFFFFFF, 64, 0.0);
		PowerFx.sound(level, player.position(), alignment == ArtifactAlignment.DARKNESS
				? SoundEvents.WITHER_SPAWN : SoundEvents.END_PORTAL_SPAWN, 4.0F,
				alignment == ArtifactAlignment.DARKNESS ? 0.45F : 1.55F);
		return true;
	}

	private static boolean eligible(ServerPlayer player, LivingEntity target) {
		return target != null && target != player && target.isAlive() && player.level() == target.level()
				&& player.distanceToSqr(target) <= TARGET_RANGE * TARGET_RANGE
				&& !AmethystDampening.isDampened(target)
				&& !SpellFieldManager.isSanctuaryProtected((ServerLevel) target.level(), target);
	}

	private static boolean inCone(Vec3 origin, Vec3 look, Vec3 point) {
		Vec3 offset = point.subtract(origin);
		if (offset.lengthSqr() > 32.0 * 32.0 || offset.lengthSqr() <= 1.0E-6) return false;
		return offset.normalize().dot(look) >= 0.72;
	}
}

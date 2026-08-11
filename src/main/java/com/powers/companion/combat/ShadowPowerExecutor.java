package com.powers.companion.combat;

import com.powers.PowerStatusEffects;
import com.powers.companion.ShadowCompanionEntity;
import com.powers.companion.ShadowMagicState;
import com.powers.entity.DarknessFireballProjectile;
import com.powers.fx.PowerFx;
import com.powers.item.artifact.ArtifactAlignment;
import com.powers.magic.participant.MagicParticipants;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.power.abilities.CombatTerrainImpact;
import com.powers.power.artifact.ArtifactGuardianSummons;
import com.powers.power.state.GlobalTimeStopManager;
import com.powers.power.state.MagicShieldManager;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Entity-safe max-Darkness executors for the complete non-crystal arsenal. */
public final class ShadowPowerExecutor {
	public enum Handler {
		MOBILITY, PROJECTILE, BEAM, AREA, CONTROL, DEFENSE, RECOVERY, TOGGLE,
		MIND, SUMMON, TERRAIN, APOTHEOSIS, UNSUPPORTED
	}

	public record ExecutionContext(ServerPlayer owner, boolean allowTerrain, long serverTick) { }
	public record ExecutionResult(boolean success, String reason, int energySpent,
			Handler handler, List<UUID> ownedEntities) {
		public ExecutionResult {
			reason = reason == null ? "" : reason;
			ownedEntities = List.copyOf(ownedEntities);
		}
	}

	private ShadowPowerExecutor() {
	}

	public static Handler handler(String id) {
		return switch (id) {
			case "time_shift", "speed_burst", "super_speed", "flight" -> Handler.MOBILITY;
			case "fireball", "lightning_strike" -> Handler.PROJECTILE;
			case "void_beam", "energy_beam", "ice_manipulation" -> Handler.BEAM;
			case "starfall", "thunderclap" -> Handler.AREA;
			case "telekinesis", "breezy_bash", "gravity_displacement", "time_freeze" -> Handler.CONTROL;
			case "forcefield", "double_health" -> Handler.DEFENSE;
			case "plant_healing_acceleration", "energy_drain" -> Handler.RECOVERY;
			case "size_shift", "invisibility" -> Handler.TOGGLE;
			case "vessel_possession", "astral_projection" -> Handler.MIND;
			case "call_hollowed" -> Handler.SUMMON;
			case "blight_ground" -> Handler.TERRAIN;
			case "nightfall_dominion" -> Handler.APOTHEOSIS;
			default -> Handler.UNSUPPORTED;
		};
	}

	public static ExecutionResult execute(ServerLevel level, ShadowCompanionEntity shadow,
			LivingEntity target, ShadowPowerAction action, ExecutionContext context) {
		Handler handler = handler(action.id());
		if (handler == Handler.UNSUPPORTED) return failed("unsupported", handler);
		if (ShadowMagicState.actionsSuppressed(shadow)) {
			AmethystDampening.punish(shadow);
			return failed("amethyst_suppressed", handler);
		}
		if (!ShadowPowerRuntime.tryReserve(action.workClass(), context.serverTick())) {
			return failed("server_work_budget", handler);
		}
		if (requiresTarget(action.id()) && (target == null || !target.isAlive()
				|| target == shadow || target == context.owner())) return failed("no_target", handler);
		if (shadow.energy() < action.cost()) return failed("insufficient_energy", handler);

		boolean success = switch (handler) {
			case MOBILITY -> mobility(level, shadow, target, action.id());
			case PROJECTILE -> projectile(level, shadow, target, action.id(), context.allowTerrain());
			case BEAM -> beam(level, shadow, target, action.id(), context.allowTerrain());
			case AREA -> area(level, shadow, target, action.id(), context.allowTerrain());
			case CONTROL -> control(level, shadow, target, action.id(), context);
			case DEFENSE -> defense(level, shadow, action.id());
			case RECOVERY -> recovery(level, shadow, target, action.id());
			case TOGGLE -> toggle(level, shadow, action.id(), context);
			case MIND -> mind(level, shadow, target, action.id());
			case SUMMON -> summon(shadow, target, context.owner());
			case TERRAIN -> terrain(level, shadow, context.allowTerrain());
			case APOTHEOSIS -> apotheosis(level, shadow, context);
			case UNSUPPORTED -> false;
		};
		if (!success) return failed("countered_or_blocked", handler);
		if (action.toggle()) ShadowPowerRuntime.activate(context.owner().getUUID(), shadow.getUUID(),
				action.id(), context.serverTick() + 1_200L);
		shadow.setEnergy(shadow.energy() - action.cost());
		ShadowPowerFx.cast(level, shadow, target, action);
		return new ExecutionResult(true, "cast", action.cost(), handler, List.of());
	}

	private static boolean mobility(ServerLevel level, ShadowCompanionEntity shadow,
			LivingEntity target, String id) {
		if (id.equals("time_shift")) {
			Vec3 direction = target.position().subtract(shadow.position());
			Vec3 destination = target.position().subtract(direction.normalize().scale(3.5));
			return shadow.randomTeleport(destination.x, destination.y, destination.z, true);
		}
		if (target == null) return false;
		Vec3 direction = target.getEyePosition().subtract(shadow.getEyePosition());
		if (direction.lengthSqr() < 1.0E-6) return false;
		double speed = id.equals("flight") ? 1.25 : 2.15;
		shadow.setNoGravity(id.equals("flight"));
		shadow.setDeltaMovement(direction.normalize().scale(speed).add(0.0, 0.25, 0.0));
		return true;
	}

	private static boolean projectile(ServerLevel level, ShadowCompanionEntity shadow,
			LivingEntity target, String id, boolean terrain) {
		if (id.equals("lightning_strike")) {
			var bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
			if (bolt == null) return false;
			bolt.setVisualOnly(false);
			bolt.setPos(target.position());
			level.addFreshEntity(bolt);
			boolean hit = harm(level, shadow, target, bossDamage(target, 72.0F, 0.18F));
			if (terrain) CombatTerrainImpact.craterLiving(level, shadow, target.position(), 5);
			return hit;
		}
		Vec3 direction = target.getEyePosition().subtract(shadow.getEyePosition()).normalize();
		DarknessFireballProjectile fireball = new DarknessFireballProjectile(level, shadow, direction);
		fireball.setPos(shadow.getEyePosition().add(direction.scale(1.4)));
		level.addFreshEntity(fireball);
		return true;
	}

	private static boolean beam(ServerLevel level, ShadowCompanionEntity shadow,
			LivingEntity target, String id, boolean terrain) {
		int color = id.equals("void_beam") ? 0x48105F
				: id.equals("ice_manipulation") ? 0x8BE9FF : 0xA1222C;
		float base = id.equals("void_beam") ? 92.0F : id.equals("energy_beam") ? 82.0F : 66.0F;
		boolean hit = harm(level, shadow, target, bossDamage(target, base, 0.2F));
		if (hit && id.equals("ice_manipulation")) target.addEffect(PowerStatusEffects.hidden(
				MobEffects.SLOWNESS, 160, 5, false, true));
		PowerFx.beam(level, shadow.getEyePosition(), target.getEyePosition(),
				id.equals("ice_manipulation") ? ParticleTypes.SNOWFLAKE : PowerFx.dust(color, 1.4F), 36);
		if (terrain) CombatTerrainImpact.rayScarLiving(level, shadow, shadow.getEyePosition(),
				target.position(), 10, color);
		return hit;
	}

	private static boolean area(ServerLevel level, ShadowCompanionEntity shadow,
			LivingEntity target, String id, boolean terrain) {
		Vec3 center = id.equals("thunderclap") ? shadow.position() : target.position();
		double radius = id.equals("thunderclap") ? 14.0 : 11.0;
		int hits = 0;
		for (LivingEntity affected : nearby(level, shadow, center, radius)) {
			if (!harm(level, shadow, affected, bossDamage(affected, 68.0F, 0.16F))) continue;
			Vec3 away = affected.position().subtract(center);
			if (away.lengthSqr() > 1.0E-6) affected.setDeltaMovement(
					away.normalize().scale(1.7).add(0.0, id.equals("starfall") ? 1.3 : 0.45, 0.0));
			hits++;
		}
		if (terrain) CombatTerrainImpact.craterLiving(level, shadow, center, 10);
		return hits > 0;
	}

	private static boolean control(ServerLevel level, ShadowCompanionEntity shadow,
			LivingEntity target, String id, ExecutionContext context) {
		if (id.equals("time_freeze")) return GlobalTimeStopManager.startShadow(context.owner(), shadow);
		if (!mayControl(level, target)) return false;
		Vec3 delta = target.position().subtract(shadow.position());
		if (id.equals("gravity_displacement")) {
			target.setDeltaMovement(delta.lengthSqr() < 1.0E-6 ? Vec3.ZERO
					: delta.normalize().scale(-1.25).add(0.0, 1.6, 0.0));
			target.addEffect(PowerStatusEffects.hidden(MobEffects.SLOWNESS, 100, 5, false, true));
		} else {
			target.setDeltaMovement(delta.lengthSqr() < 1.0E-6 ? Vec3.ZERO
					: delta.normalize().scale(2.1).add(0.0, 1.0, 0.0));
		}
		return harm(level, shadow, target, bossDamage(target, 54.0F, 0.12F));
	}

	private static boolean defense(ServerLevel level, ShadowCompanionEntity shadow, String id) {
		if (id.equals("forcefield")) {
			MagicShieldManager.global().raise(shadow.getUUID(), 500.0F,
					level.getServer().getTickCount() + 20L * 60L * 5L, true);
		} else {
			shadow.addEffect(PowerStatusEffects.hidden(MobEffects.ABSORPTION, 1_200, 9, false, true));
			shadow.addEffect(PowerStatusEffects.hidden(MobEffects.RESISTANCE, 1_200, 2, false, true));
		}
		return true;
	}

	private static boolean recovery(ServerLevel level, ShadowCompanionEntity shadow,
			LivingEntity target, String id) {
		if (id.equals("plant_healing_acceleration")) {
			shadow.heal(Math.max(30.0F, shadow.getMaxHealth() * 0.65F));
			shadow.removeEffect(MobEffects.WITHER);
			return true;
		}
		if (target == null || !target.isAlive()) return false;
		int drained = MagicParticipants.resolve(target).map(participant -> {
			int before = participant.energy();
			participant.consume(Math.min(160, before));
			return before - participant.energy();
		}).orElseGet(() -> harm(level, shadow, target,
				bossDamage(target, 64.0F, 0.14F)) ? 80 : 0);
		shadow.setEnergy(shadow.energy() + drained);
		shadow.heal(Math.max(4.0F, drained * 0.18F));
		return drained > 0;
	}

	private static boolean toggle(ServerLevel level, ShadowCompanionEntity shadow,
			String id, ExecutionContext context) {
		if (id.equals("size_shift")) {
			shadow.getAttribute(Attributes.SCALE).setBaseValue(1.75);
		} else {
			shadow.addEffect(PowerStatusEffects.hidden(MobEffects.INVISIBILITY, 1_200, 0, false, true));
		}
		ShadowPowerRuntime.activate(context.owner().getUUID(), shadow.getUUID(), id,
				context.serverTick() + 1_200L);
		return true;
	}

	private static boolean mind(ServerLevel level, ShadowCompanionEntity shadow,
			LivingEntity target, String id) {
		if (!mayControl(level, target)) return false;
		Vec3 destination = id.equals("vessel_possession") ? target.position()
				: target.position().subtract(target.getLookAngle().scale(3.0));
		boolean moved = shadow.randomTeleport(destination.x, destination.y, destination.z, true);
		if (moved) {
			target.addEffect(PowerStatusEffects.hidden(MobEffects.DARKNESS, 80, 0, false, true));
			harm(level, shadow, target, bossDamage(target, 48.0F, 0.1F));
		}
		return moved;
	}

	private static boolean summon(ShadowCompanionEntity shadow, LivingEntity target,
			ServerPlayer owner) {
		return ArtifactGuardianSummons.summonLiving(shadow, ArtifactAlignment.DARKNESS,
				3, false, target, true) > 0;
	}

	private static boolean terrain(ServerLevel level, ShadowCompanionEntity shadow,
			boolean allowTerrain) {
		return allowTerrain && CombatTerrainImpact.blightDisc(level, shadow, 8, 96) > 0;
	}

	private static boolean apotheosis(ServerLevel level, ShadowCompanionEntity shadow,
			ExecutionContext context) {
		shadow.addEffect(PowerStatusEffects.hidden(MobEffects.STRENGTH, 1_200, 5, false, true));
		shadow.addEffect(PowerStatusEffects.hidden(MobEffects.RESISTANCE, 1_200, 3, false, true));
		shadow.addEffect(PowerStatusEffects.hidden(MobEffects.SPEED, 1_200, 4, false, true));
		shadow.addEffect(PowerStatusEffects.hidden(MobEffects.REGENERATION, 1_200, 2, false, true));
		ShadowPowerRuntime.activate(context.owner().getUUID(), shadow.getUUID(),
				"nightfall_dominion", context.serverTick() + 1_200L);
		return true;
	}

	private static boolean harm(ServerLevel level, ShadowCompanionEntity shadow,
			LivingEntity target, float damage) {
		if (target == null || PowerProtection.isSafeZone(level, target.position())
				|| SpellFieldManager.isSanctuaryProtected(level, target)) return false;
		float adjusted = AmethystDampening.isDampened(target) ? damage * 0.35F : damage;
		return target.hurtServer(level, PowerDamage.source(shadow), adjusted);
	}

	private static boolean mayControl(ServerLevel level, LivingEntity target) {
		return target != null && !PowerProtection.isSafeZone(level, target.position())
				&& !SpellFieldManager.isSanctuaryProtected(level, target)
				&& !AmethystDampening.isDampened(target);
	}

	private static float bossDamage(LivingEntity target, float base, float healthFraction) {
		return Math.min(Math.max(base, target.getMaxHealth() * healthFraction), 240.0F);
	}

	private static List<LivingEntity> nearby(ServerLevel level, ShadowCompanionEntity shadow,
			Vec3 center, double radius) {
		return BoundedEntityCandidates.living(level,
				AABB.ofSize(center, radius * 2.0, radius * 2.0, radius * 2.0), 96,
				entity -> entity != shadow && entity.isAlive()
						&& !entity.getUUID().equals(shadow.ownerId())
						&& entity.distanceToSqr(center) <= radius * radius,
				Comparator.comparingDouble(entity -> entity.distanceToSqr(center)));
	}

	private static boolean requiresTarget(String id) {
		return !java.util.Set.of("size_shift", "flight", "speed_burst", "super_speed",
				"invisibility", "time_freeze", "forcefield", "double_health",
				"plant_healing_acceleration", "call_hollowed", "blight_ground",
				"nightfall_dominion").contains(id);
	}

	private static ExecutionResult failed(String reason, Handler handler) {
		return new ExecutionResult(false, reason, 0, handler, List.of());
	}
}

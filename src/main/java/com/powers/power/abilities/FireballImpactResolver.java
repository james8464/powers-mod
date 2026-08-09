package com.powers.power.abilities;

import com.powers.config.PowersConfigLoader;
import com.powers.fx.FireballFx;
import com.powers.mind.BodyProxyManager;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.power.state.EntityFreezeController;
import com.powers.power.state.MagicShieldManager;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** Resolves protected terminals, area effects, pressure, and terrain for one impact. */
final class FireballImpactResolver {
	private static final int MAX_COUNTER_CUES = 8;
	private static final float BASE_DAMAGE = 6.0F;
	private static final double STEAM_DAMAGE_MULTIPLIER = 0.55;
	private static final double STEAM_PRESSURE = 0.78;
	private static final double EMPOWERED_PRESSURE = 0.96;
	private static final double STEAM_LIFT = 0.18;
	private static final double EMPOWERED_LIFT = 0.24;

	private FireballImpactResolver() {
	}

	/** Resolves the first hit while preserving a legal forcefield reflection. */
	static void resolve(ServerLevel level, LargeFireball projectile,
			Cinderheart heart, HitResult hit) {
		Vec3 point = hit == null ? projectile.position() : hit.getLocation();
		ServerPlayer controller = level.getServer().getPlayerList().getPlayer(heart.controller);
		SpellFieldManager.RayWardHit ward = controller == null ? null
				: SpellFieldManager.firstHarmfulRayIntercept(level, heart.controller,
						heart.lastPosition, point).orElse(null);
		FireballRules.ImpactDecision wardDecision = wardDecision(ward);
		if (wardDecision != null) {
			FireballFx.terminal(level, ward.point(), wardDecision, heart.tier);
			FireballAbility.finishImpact(level, projectile, heart, ward.point());
			return;
		}

		LivingEntity directTarget = hit instanceof EntityHitResult entityHit
				&& entityHit.getEntity() instanceof LivingEntity living ? living : null;
		BlockState block = hit instanceof BlockHitResult blockHit
				? level.getBlockState(blockHit.getBlockPos())
				: level.getBlockState(BlockPos.containing(point));
		FireballRules.ImpactDecision decision = FireballRules.impactDecision(
				controller != null,
				PowerProtection.isSafeZone(level,
						directTarget == null ? point : directTarget.position()),
				directTarget != null ? AmethystDampening.isDampened(directTarget)
						: block.is(AmethystDampening.AMETHYST_BLOCKS),
				directTarget != null && SpellFieldManager.isSanctuaryProtected(level, directTarget),
				false, hasForcefield(level, directTarget),
				(directTarget != null && directTarget.isInWater())
						|| block.getFluidState().is(FluidTags.WATER),
				block.is(BlockTags.ICE) || block.is(BlockTags.SNOW));

		if (decision == FireballRules.ImpactDecision.FORCEFIELD
				&& directTarget != null && controller != null) {
			resolveDirectForcefield(level, projectile, heart, controller, directTarget, point);
			return;
		}
		if (decision == FireballRules.ImpactDecision.WATER
				|| decision == FireballRules.ImpactDecision.FROST) {
			resolveSteam(level, projectile, heart, point,
					decision == FireballRules.ImpactDecision.FROST);
			FireballAbility.finishImpact(level, projectile, heart, point);
			return;
		}
		if (decision != FireballRules.ImpactDecision.DETONATE) {
			FireballFx.terminal(level, point, decision, heart.tier);
			FireballAbility.finishImpact(level, projectile, heart, point);
			return;
		}

		int affected = resolveArea(level, heart, controller, point, false);
		double radius = FireballRules.impactRadius(heart.tier, heart.empoweredImpact);
		FireballFx.impact(level, point, radius, heart.tier, affected,
				heart.empoweredImpact, heart.ancientMastery);
		if (controller != null) scorchTerrain(level, controller, point, heart);
		FireballAbility.finishImpact(level, projectile, heart, point);
	}

	/** Resolves an in-flight water or frost transformation without ignition. */
	static void resolveSteam(ServerLevel level, LargeFireball projectile,
			Cinderheart heart, Vec3 point, boolean frost) {
		ServerPlayer controller = level.getServer().getPlayerList().getPlayer(heart.controller);
		resolveArea(level, heart, controller, point, true);
		FireballFx.steam(level, point,
				FireballRules.impactRadius(heart.tier, heart.empoweredImpact), frost, heart.tier);
	}

	/** Allows the shield damage bridge to spend its one ranked reflection. */
	private static void resolveDirectForcefield(ServerLevel level, LargeFireball projectile,
			Cinderheart heart, ServerPlayer controller, LivingEntity target, Vec3 point) {
		target.hurtServer(level, PowerDamage.projectileSource(controller, projectile),
				impactDamage(heart));
		if (FireballAbility.observeExternalController(projectile, heart)) {
			Entity currentOwner = projectile.getOwner();
			if (currentOwner instanceof ServerPlayer player
					&& player.getUUID().equals(heart.controller) && player != controller) {
				heart.lastPosition = projectile.position();
				return;
			}
		}
		FireballFx.terminal(level, point, FireballRules.ImpactDecision.FORCEFIELD, heart.tier);
		FireballAbility.finishImpact(level, projectile, heart, point);
	}

	/** Applies capped, nearest-first area damage and consent-safe pressure. */
	private static int resolveArea(ServerLevel level, Cinderheart heart,
			ServerPlayer controller, Vec3 center, boolean steam) {
		if (controller == null) return 0;
		double radius = FireballRules.impactRadius(heart.tier, heart.empoweredImpact);
		AABB bounds = AABB.ofSize(center, radius * 2.0, radius * 2.0, radius * 2.0);
		List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, bounds,
				entity -> entity.isAlive() && entity != controller
						&& entity.position().distanceToSqr(center) <= radius * radius);
		candidates.sort(Comparator.comparingDouble((LivingEntity entity) ->
				entity.position().distanceToSqr(center)).thenComparing(
				entity -> entity.getUUID().toString()));
		int affected = 0;
		int counterCues = 0;
		int limit = Math.min(FireballRules.targetLimit(heart.ancientMastery), candidates.size());
		for (int index = 0; index < limit; index++) {
			LivingEntity target = candidates.get(index);
			double falloff = FireballRules.falloff(
					Math.sqrt(target.position().distanceToSqr(center)), radius);
			FireballRules.ImpactDecision counter = targetCounter(level, controller, target);
			if (counter != FireballRules.ImpactDecision.DETONATE) {
				if (counter == FireballRules.ImpactDecision.FORCEFIELD) {
					float pressure = (float) (impactDamage(heart) * falloff
							* (steam ? STEAM_DAMAGE_MULTIPLIER : 1.0));
					target.hurtServer(level, PowerDamage.source(controller), pressure);
				}
				if (counterCues++ < MAX_COUNTER_CUES) {
					FireballFx.terminal(level, bodyCenter(target), counter, heart.tier);
				}
				continue;
			}
			float damage = (float) (impactDamage(heart) * falloff
					* (steam ? STEAM_DAMAGE_MULTIPLIER : 1.0));
			boolean hurt = damage > 0.0F
					&& target.hurtServer(level, PowerDamage.source(controller), damage);
			if (hurt && !steam) target.igniteForSeconds(FireballRules.burnSeconds(heart.tier));
			if (hurt) affected++;
			if ((steam || heart.empoweredImpact) && target.isAlive()) {
				applyPressure(level, controller, target, center, falloff, steam, index);
			}
		}
		return affected;
	}

	/** Classifies per-body splash protection independently from material impact. */
	private static FireballRules.ImpactDecision targetCounter(ServerLevel level,
			ServerPlayer controller, LivingEntity target) {
		return FireballRules.impactDecision(true,
				!PowerProtection.mayHarm(controller, target),
				AmethystDampening.isDampened(target),
				SpellFieldManager.isSanctuaryProtected(level, target), false,
				hasForcefield(level, target), false, false);
	}

	/** Writes pressure only after consent, anchor, ward, time, shield, and collision checks. */
	private static void applyPressure(ServerLevel level, ServerPlayer controller,
			LivingEntity target, Vec3 center, double falloff, boolean steam, int index) {
		if (!PowerProtection.mayForceMove(controller, target)
				|| BodyProxyManager.isProxy(target) || EntityFreezeController.isFrozen(target)
				|| SpellFieldManager.blocksForcedMovement(level, target, controller.getUUID())
				|| hasForcefield(level, target)) return;
		double horizontal = (steam ? STEAM_PRESSURE : EMPOWERED_PRESSURE) * falloff;
		Vec3 impulse = FireballRules.pressureImpulse(center, target.position(), horizontal,
				steam ? STEAM_LIFT : EMPOWERED_LIFT);
		Vec3 velocity = target.getDeltaMovement().scale(0.30).add(impulse);
		if (impulse.equals(Vec3.ZERO)
				|| !level.noBlockCollision(target, target.getBoundingBox().move(velocity))) return;
		target.setDeltaMovement(velocity);
		target.hurtMarked = true;
		target.fallDistance = 0.0F;
		FireballFx.pressureTarget(level, center, bodyCenter(target), steam, index);
	}

	/** Places only bounded ordinary fire when server terrain policy explicitly permits it. */
	private static void scorchTerrain(ServerLevel level, ServerPlayer controller,
			Vec3 center, Cinderheart heart) {
		int limit = FireballRules.terrainScorchLimit(
				heart.tier, PowersConfigLoader.get().allowTerrainDamage());
		if (limit <= 0) return;
		BlockPos origin = BlockPos.containing(center);
		int placed = 0;
		for (int dy = 0; dy <= 1 && placed < limit; dy++) {
			for (int dx = -1; dx <= 1 && placed < limit; dx++) {
				for (int dz = -1; dz <= 1 && placed < limit; dz++) {
					BlockPos pos = origin.offset(dx, dy, dz);
					BlockState fire = Blocks.FIRE.defaultBlockState();
					if (!level.getBlockState(pos).isAir() || !fire.canSurvive(level, pos)
							|| !PowerProtection.mayAffectBlock(controller, level, pos)) continue;
					level.setBlockAndUpdate(pos, fire);
					FireballFx.terrainScorch(level, Vec3.atCenterOf(pos), placed++);
				}
			}
		}
	}

	private static FireballRules.ImpactDecision wardDecision(
			SpellFieldManager.RayWardHit ward) {
		if (ward == null) return null;
		return switch (ward.counterplay()) {
			case SANCTUARY -> FireballRules.ImpactDecision.SANCTUARY;
			case KINETIC_WARD -> FireballRules.ImpactDecision.KINETIC_WARD;
			default -> null;
		};
	}

	private static boolean hasForcefield(ServerLevel level, LivingEntity target) {
		return target instanceof ServerPlayer player
				&& MagicShieldManager.global().active(
						player.getUUID(), level.getServer().getTickCount());
	}

	private static float impactDamage(Cinderheart heart) {
		return (float) (BASE_DAMAGE * heart.potencyMultiplier
				* FireballRules.damageMultiplier(heart.tier, heart.empoweredImpact));
	}

	private static Vec3 bodyCenter(LivingEntity entity) {
		return entity.position().add(0.0, entity.getBbHeight() * 0.52, 0.0);
	}
}

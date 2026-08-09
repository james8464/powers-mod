package com.powers.power.abilities;

import com.powers.PowersBlocks;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerTargeting;
import com.powers.power.state.MagicShieldManager;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Resolves finite Sunfire ray terminals and protected nearby target sets. */
final class EnergyBeamRayResolver {
	/** The nearest endpoint, with either a body or a semantic terminal. */
	record RayResolution(Vec3 endpoint, LivingEntity target,
			EnergyBeamRules.Counterplay counterplay) {
	}

	private EnergyBeamRayResolver() {
	}

	/** Recasts the player's server-owned aim against bodies, matter, wards, and water. */
	static RayResolution resolve(ServerLevel level, ServerPlayer caster, double range) {
		Vec3 origin = caster.getEyePosition();
		Vec3 direction = caster.getLookAngle();
		if (!finiteDirection(direction)) return new RayResolution(origin, null, null);
		direction = direction.normalize();
		HitResult picked = PowerTargeting.raycast(caster, range);
		double pickedDistance = picked.getType() == HitResult.Type.MISS
				? range : Math.min(range, origin.distanceTo(picked.getLocation()));
		LivingEntity target = picked instanceof EntityHitResult entityHit
				&& entityHit.getEntity() instanceof LivingEntity living ? living : null;
		List<EnergyBeamRules.Intercept> intercepts = new ArrayList<>();
		if (picked instanceof BlockHitResult blockHit && picked.getType() == HitResult.Type.BLOCK) {
			intercepts.add(new EnergyBeamRules.Intercept(
					blockCounter(level, blockHit), pickedDistance));
		}
		Vec3 pickedEnd = origin.add(direction.scale(pickedDistance));
		SpellFieldManager.firstHarmfulRayIntercept(level, caster.getUUID(), origin, pickedEnd)
				.ifPresent(hit -> intercepts.add(new EnergyBeamRules.Intercept(
						wardCounter(hit.counterplay()), hit.distance())));
		intercepts.addAll(environmentalIntercepts(level, origin, direction, pickedDistance));
		Optional<EnergyBeamRules.Intercept> terminal = EnergyBeamRules
				.nearestTerminal(intercepts, pickedDistance);
		if (target != null && terminal.isEmpty()) {
			return new RayResolution(picked.getLocation(), target, null);
		}
		if (terminal.isPresent()) {
			EnergyBeamRules.Intercept hit = terminal.get();
			return new RayResolution(origin.add(direction.scale(hit.distance())),
					null, hit.counterplay());
		}
		return new RayResolution(pickedEnd, null, null);
	}

	/** Returns nearest visible bodies in deterministic distance/UUID order. */
	static List<LivingEntity> nearbyTargets(ServerLevel level, ServerPlayer caster,
			Vec3 center, double radius, int scanLimit, LivingEntity excluded) {
		AABB bounds = AABB.ofSize(center, radius * 2.0, radius * 2.0, radius * 2.0);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, bounds,
				entity -> entity.isAlive() && entity != caster && entity != excluded
						&& !entity.isSpectator() && caster.hasLineOfSight(entity)
						&& entity.position().distanceToSqr(center) <= radius * radius);
		targets.sort(Comparator.comparingDouble((LivingEntity entity) ->
				entity.position().distanceToSqr(center)).thenComparing(
				entity -> entity.getUUID().toString()));
		return targets.subList(0, Math.min(Math.max(0, scanLimit), targets.size()));
	}

	/** Resolves body-local and intervening protections before damage is attempted. */
	static EnergyBeamRules.Counterplay targetCounter(ServerLevel level,
			ServerPlayer caster, LivingEntity target, Vec3 rayOrigin) {
		if (AmethystDampening.isDampened(target)) return EnergyBeamRules.Counterplay.AMETHYST;
		if (!PowerProtection.mayHarm(caster, target)) return EnergyBeamRules.Counterplay.SAFE_ZONE;
		if (SpellFieldManager.isSanctuaryProtected(level, target)) {
			return EnergyBeamRules.Counterplay.SANCTUARY;
		}
		Optional<SpellFieldManager.RayWardHit> ward = SpellFieldManager.firstHarmfulRayIntercept(
				level, caster.getUUID(), rayOrigin, bodyCenter(target));
		if (ward.isPresent()) return wardCounter(ward.get().counterplay());
		if (target instanceof ServerPlayer player && MagicShieldManager.global()
				.active(player.getUUID(), level.getServer().getTickCount())) {
			return EnergyBeamRules.Counterplay.FORCEFIELD;
		}
		return null;
	}

	/** Returns the stable centre used by rays and counter ceremonies. */
	static Vec3 bodyCenter(LivingEntity target) {
		return target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
	}

	private static List<EnergyBeamRules.Intercept> environmentalIntercepts(ServerLevel level,
			Vec3 origin, Vec3 direction, double maximumDistance) {
		List<EnergyBeamRules.Intercept> result = new ArrayList<>(2);
		int samples = EnergyBeamRules.waterSamples(maximumDistance);
		boolean foundSafeZone = PowerProtection.isSafeZone(level, origin);
		if (foundSafeZone) {
			result.add(new EnergyBeamRules.Intercept(
					EnergyBeamRules.Counterplay.SAFE_ZONE, 0.0));
		}
		BlockPos originPos = BlockPos.containing(origin);
		boolean foundWater = LoadedChunks.contains(level, originPos)
				&& level.getFluidState(originPos).is(FluidTags.WATER);
		if (foundWater) {
			result.add(new EnergyBeamRules.Intercept(
					EnergyBeamRules.Counterplay.WATER, 0.0));
		}
		for (int index = 1; index <= samples && (!foundWater || !foundSafeZone); index++) {
			double distance = maximumDistance * index / samples;
			Vec3 point = origin.add(direction.scale(distance));
			if (!foundSafeZone && PowerProtection.isSafeZone(level, point)) {
				result.add(new EnergyBeamRules.Intercept(
						EnergyBeamRules.Counterplay.SAFE_ZONE, distance));
				foundSafeZone = true;
			}
			BlockPos pos = BlockPos.containing(point);
			if (!foundWater && LoadedChunks.contains(level, pos)
					&& level.getFluidState(pos).is(FluidTags.WATER)) {
				result.add(new EnergyBeamRules.Intercept(
						EnergyBeamRules.Counterplay.WATER, distance));
				foundWater = true;
			}
		}
		return result;
	}

	private static EnergyBeamRules.Counterplay blockCounter(ServerLevel level,
			BlockHitResult hit) {
		BlockState state = level.getBlockState(hit.getBlockPos());
		if (state.is(AmethystDampening.AMETHYST_BLOCKS)) {
			return EnergyBeamRules.Counterplay.AMETHYST;
		}
		if (state.is(PowersBlocks.PURE_LIGHT)) return EnergyBeamRules.Counterplay.PURE_LIGHT;
		if (state.is(PowersBlocks.DARKNESS)) return EnergyBeamRules.Counterplay.DARKNESS;
		return EnergyBeamRules.Counterplay.SURFACE;
	}

	private static EnergyBeamRules.Counterplay wardCounter(
			VoidBeamRules.Counterplay counterplay) {
		return counterplay == VoidBeamRules.Counterplay.SANCTUARY
				? EnergyBeamRules.Counterplay.SANCTUARY
				: EnergyBeamRules.Counterplay.KINETIC_WARD;
	}

	private static boolean finiteDirection(Vec3 direction) {
		return direction != null && Double.isFinite(direction.x) && Double.isFinite(direction.y)
				&& Double.isFinite(direction.z) && direction.lengthSqr() > 1.0E-8;
	}
}

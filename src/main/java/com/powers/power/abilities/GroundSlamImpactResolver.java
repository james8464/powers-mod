package com.powers.power.abilities;

import com.powers.PowerStatusEffects;
import com.powers.PowersBlocks;
import com.powers.config.PowersConfigLoader;
import com.powers.fx.GroundSlamFx;
import com.powers.mind.BodyProxyManager;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.power.state.EntityFreezeController;
import com.powers.power.state.MagicShieldManager;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.LoadedChunks;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Resolves surfaces, matter, wards, bodies, pressure, and terrain for one fault beat. */
final class GroundSlamImpactResolver {
	private static final int MAX_COUNTER_CUES = 6;
	private static final int MAX_HIT_HISTORY = 72;
	private static final double BASE_PRESSURE = 1.25;
	private static final double BASE_LIFT = 0.34;

	/** One located support surface and its environment result. */
	record StrikeSite(Vec3 point, BlockPos support,
			GroundSlamRules.Counterplay counterplay) {
	}

	/** One completed beat with enough evidence for lifecycle and rank effects. */
	record ImpactResult(Vec3 point, int affected,
			GroundSlamRules.Counterplay counterplay) {
	}

	private GroundSlamImpactResolver() {
	}

	/** Locates the initial supporting surface without loading a chunk. */
	static StrikeSite initialSite(ServerLevel level, Vec3 requested) {
		return locate(level, requested);
	}

	/** Returns the current loaded epicentre used by the warning presentation. */
	static Vec3 previewPoint(ServerLevel level, Vec3 requested) {
		StrikeSite site = locate(level, requested);
		return site == null ? requested : site.point();
	}

	/** Resolves one primary, Communion, or Dominion beat without vanilla explosion grief. */
	static ImpactResult resolve(ServerLevel level, ServerPlayer caster,
			FaultboundVerdict rite, Vec3 requested, GroundSlamRules.Beat beat) {
		StrikeSite site = locate(level, requested);
		if (site == null) {
			GroundSlamFx.counter(level, finite(requested) ? requested : caster.position(),
					GroundSlamRules.Counterplay.UNLOADED);
			return new ImpactResult(requested, 0, GroundSlamRules.Counterplay.UNLOADED);
		}
		GroundSlamRules.Counterplay medium = site.counterplay();
		if (!GroundSlamRules.impactAllowed(medium)) {
			GroundSlamFx.counter(level, site.point(), medium);
			return new ImpactResult(site.point(), 0, medium);
		}

		double radius = GroundSlamRules.impactRadius(
				rite.baseRadius, beat, rite.empoweredImpact);
		int affected = damageTargets(level, caster, rite, site, radius, beat);
		GroundSlamFx.impact(level, site.point(), radius, beat, medium,
				affected, rite.empoweredImpact);
		if (beat == GroundSlamRules.Beat.PRIMARY
				&& medium == GroundSlamRules.Counterplay.IMPACT) {
			fractureTerrain(level, caster, rite, site, radius);
		}
		return new ImpactResult(site.point(), affected, medium);
	}

	/** Finds the nearest loaded support within four blocks below the requested centre. */
	private static StrikeSite locate(ServerLevel level, Vec3 requested) {
		if (!finite(requested)) return null;
		BlockPos requestedPos = BlockPos.containing(requested.add(0.0, 0.25, 0.0));
		if (!level.getWorldBorder().isWithinBounds(requestedPos)
				|| !LoadedChunks.contains(level, requestedPos)) return null;

		BlockPos support = requestedPos;
		BlockState state = level.getBlockState(support);
		boolean supported = false;
		for (int offset = 0; offset <= 4; offset++) {
			BlockPos candidate = requestedPos.below(offset);
			BlockState candidateState = level.getBlockState(candidate);
			if (candidateState.getCollisionShape(level, candidate).isEmpty()
					&& candidateState.getFluidState().isEmpty()) continue;
			support = candidate;
			state = candidateState;
			supported = true;
			break;
		}
		double surface = 1.0;
		if (supported && state.getFluidState().isEmpty()) {
			var shape = state.getCollisionShape(level, support);
			if (!shape.isEmpty()) surface = shape.bounds().maxY;
		}
		Vec3 point = supported
				? new Vec3(requested.x, support.getY() + surface + 0.02, requested.z)
				: requested;
		boolean amethyst = state.is(AmethystDampening.AMETHYST_BLOCKS)
				|| AmethystDampening.findPoweredWard(level, support).isPresent();
		GroundSlamRules.Counterplay counterplay = GroundSlamRules.environmentDecision(
				true, PowerProtection.isSafeZone(level, point), amethyst,
				state.is(PowersBlocks.DARKNESS),
				state.getFluidState().is(FluidTags.WATER),
				state.is(PowersBlocks.PURE_LIGHT), supported);
		return new StrikeSite(point, support, counterplay);
	}

	/** Applies nearest-first capped damage and separately protected seismic pressure. */
	private static int damageTargets(ServerLevel level, ServerPlayer caster,
			FaultboundVerdict rite, StrikeSite site, double radius,
			GroundSlamRules.Beat beat) {
		Vec3 center = site.point();
		AABB bounds = AABB.ofSize(center.add(0.0, 1.0, 0.0),
				radius * 2.0, 8.0, radius * 2.0);
		List<LivingEntity> candidates = BoundedEntityCandidates.living(level, bounds, 192,
				entity -> entity.isAlive() && entity != caster && !entity.isSpectator()
						&& Math.abs(entity.getY() - center.y) <= 4.0
						&& horizontalDistanceSquared(entity.position(), center)
								<= radius * radius);
		candidates.sort(Comparator.comparingDouble((LivingEntity entity) ->
				horizontalDistanceSquared(entity.position(), center)).thenComparing(
				entity -> entity.getUUID().toString()));

		int limit = Math.min(GroundSlamRules.targetLimit(
				rite.empoweredImpact, rite.ancientMastery), candidates.size());
		int affected = 0;
		int counterCues = 0;
		long now = level.getServer().getTickCount();
		for (int index = 0; index < limit; index++) {
			LivingEntity target = candidates.get(index);
			if (rite.hits.getOrDefault(target.getUUID(), 0) >= 3) continue;
			double distance = Math.sqrt(horizontalDistanceSquared(target.position(), center));
			double falloff = GroundSlamRules.falloff(distance, radius);
			boolean grounded = BodyProxyManager.isProxy(target)
					|| target.onGround() || target.isInWater();
			GroundSlamRules.Counterplay targetMedium = GroundSlamRules.targetMedium(
					site.counterplay(), target.isInWater());
			float damage = (float) (rite.baseDamage * falloff
					* GroundSlamRules.damageMultiplier(beat, rite.empoweredImpact,
							grounded, targetMedium));
			GroundSlamRules.Counterplay counter = targetCounter(
					level, caster, target, center, now);
			if (counter != GroundSlamRules.Counterplay.IMPACT) {
				if (counter == GroundSlamRules.Counterplay.FORCEFIELD && damage > 0.0F) {
					target.hurtServer(level, PowerDamage.source(caster), damage);
					recordHit(rite, target.getUUID());
				}
				if (counterCues++ < MAX_COUNTER_CUES) {
					GroundSlamFx.counter(level, bodyCenter(target), counter);
				}
				continue;
			}

			if (damage <= 0.0F
					|| !target.hurtServer(level, PowerDamage.source(caster), damage)) {
				if (counterCues++ < MAX_COUNTER_CUES) {
					GroundSlamFx.counter(level, bodyCenter(target),
							GroundSlamRules.Counterplay.RESISTED);
				}
				continue;
			}
			recordHit(rite, target.getUUID());
			affected++;
			if (rite.trueSight) reveal(level, target);
			if (target.isAlive()) {
				GroundSlamRules.Counterplay pressure = applyPressure(level, caster,
						target, center, falloff, grounded, targetMedium, beat,
						rite.empoweredImpact, index);
				if (pressure != GroundSlamRules.Counterplay.IMPACT
						&& counterCues++ < MAX_COUNTER_CUES) {
					GroundSlamFx.counter(level, bodyCenter(target), pressure);
				}
			}
		}
		return affected;
	}

	/** Resolves harm wards crossed by the earth path and protection at the body. */
	private static GroundSlamRules.Counterplay targetCounter(ServerLevel level,
			ServerPlayer caster, LivingEntity target, Vec3 center, long now) {
		SpellFieldManager.RayWardHit ward = SpellFieldManager.firstHarmfulRayIntercept(
				level, caster.getUUID(), center.add(0.0, 0.24, 0.0), bodyCenter(target))
				.orElse(null);
		boolean sanctuary = SpellFieldManager.isSanctuaryProtected(level, target)
				|| ward != null && ward.counterplay() == VoidBeamRules.Counterplay.SANCTUARY;
		boolean kineticWard = ward != null
				&& ward.counterplay() == VoidBeamRules.Counterplay.KINETIC_WARD;
		boolean forcefield = target instanceof ServerPlayer player
				&& MagicShieldManager.global().active(player.getUUID(), now);
		return GroundSlamRules.bodyDecision(PowerProtection.mayHarm(caster, target),
				bodyAmethyst(level, target), sanctuary, kineticWard, forcefield);
	}

	/** Writes pressure only after every movement protection and collision check. */
	private static GroundSlamRules.Counterplay applyPressure(ServerLevel level,
			ServerPlayer caster, LivingEntity target, Vec3 center, double falloff,
			boolean grounded, GroundSlamRules.Counterplay medium,
			GroundSlamRules.Beat beat, boolean empoweredImpact, int index) {
		double scale = GroundSlamRules.pressureMultiplier(beat,
				empoweredImpact, grounded, medium) * falloff;
		Vec3 impulse = GroundSlamRules.pressureImpulse(center, target.position(),
				BASE_PRESSURE * scale, BASE_LIFT * Math.min(1.35, scale + 0.25));
		Vec3 velocity = target.getDeltaMovement().scale(0.28).add(impulse);
		boolean clearPath = !impulse.equals(Vec3.ZERO)
				&& level.noBlockCollision(target, target.getBoundingBox().move(velocity));
		boolean forcefield = target instanceof ServerPlayer player
				&& MagicShieldManager.global().active(player.getUUID(),
						level.getServer().getTickCount());
		GroundSlamRules.Counterplay decision = GroundSlamRules.pressureDecision(
				PowerProtection.mayForceMove(caster, target),
				bodyAmethyst(level, target), BodyProxyManager.isProxy(target),
				forcefield, SpellFieldManager.blocksForcedMovement(
						level, target, caster.getUUID()),
				EntityFreezeController.isFrozen(target), clearPath);
		if (decision != GroundSlamRules.Counterplay.IMPACT) return decision;
		target.setDeltaMovement(velocity);
		target.hurtMarked = true;
		target.fallDistance = 0.0F;
		GroundSlamFx.pressure(level, center, bodyCenter(target), index);
		return GroundSlamRules.Counterplay.IMPACT;
	}

	/** Removes only deterministic policy-approved soft blocks, without item drops. */
	private static void fractureTerrain(ServerLevel level, ServerPlayer caster,
			FaultboundVerdict rite, StrikeSite site, double radius) {
		int limit = GroundSlamRules.terrainLimit(
				PowersConfigLoader.get().allowTerrainDamage(), true, rite.ancientMastery);
		int removed = 0;
		for (int index = 0; index < limit; index++) {
			Vec3 offset = GroundSlamRules.terrainOffset(index, limit, radius * 0.72);
			BlockPos column = BlockPos.containing(site.point().add(offset));
			BlockPos pos = findSoftSurface(level, column, site.support().getY());
			if (pos == null) continue;
			BlockState state = level.getBlockState(pos);
			if (!PowerProtection.mayAffectBlock(caster, level, pos)
					|| state.is(AmethystDampening.AMETHYST_BLOCKS)
					|| state.is(PowersBlocks.DARKNESS) || state.is(PowersBlocks.PURE_LIGHT)
					|| state.is(Blocks.BEDROCK) || !state.getFluidState().isEmpty()) continue;
			float hardness = state.getDestroySpeed(level, pos);
			if (!Float.isFinite(hardness) || hardness < 0.0F || hardness > 3.0F) continue;
			if (!level.destroyBlock(pos, false, caster)) continue;
			GroundSlamFx.terrainFracture(level, Vec3.atCenterOf(pos), removed++);
		}
	}

	/** Finds one collision-bearing surface near the authored epicentre height. */
	private static BlockPos findSoftSurface(ServerLevel level, BlockPos column, int supportY) {
		for (int offset = 1; offset >= -2; offset--) {
			BlockPos pos = new BlockPos(column.getX(), supportY + offset, column.getZ());
			if (!LoadedChunks.contains(level, pos)) return null;
			BlockState state = level.getBlockState(pos);
			if (!state.getCollisionShape(level, pos).isEmpty()) return pos;
		}
		return null;
	}

	/** Insight reveals only after the body actually accepts damage. */
	private static void reveal(ServerLevel level, LivingEntity target) {
		boolean concealed = target.isInvisible();
		if (target instanceof ServerPlayer player) concealed |= InvisibilityToggleAbility.reveal(player);
		target.addEffect(PowerStatusEffects.hidden(MobEffects.GLOWING, 80, 0, true, true));
		if (concealed) GroundSlamFx.revelation(level, bodyCenter(target));
	}

	private static void recordHit(FaultboundVerdict rite, UUID target) {
		if (!rite.hits.containsKey(target) && rite.hits.size() >= MAX_HIT_HISTORY) {
			UUID eldest = rite.hits.keySet().iterator().next();
			rite.hits.remove(eldest);
		}
		rite.hits.merge(target, 1, Integer::sum);
	}

	/** Extends tagged and indexed amethyst protection to non-player living bodies. */
	private static boolean bodyAmethyst(ServerLevel level, LivingEntity target) {
		BlockPos feet = target.blockPosition();
		return AmethystDampening.isDampened(target)
				|| level.getBlockState(feet).is(AmethystDampening.AMETHYST_BLOCKS)
				|| level.getBlockState(feet.below()).is(AmethystDampening.AMETHYST_BLOCKS)
				|| AmethystDampening.findPoweredWard(level, feet).isPresent();
	}

	private static double horizontalDistanceSquared(Vec3 first, Vec3 second) {
		double x = first.x - second.x;
		double z = first.z - second.z;
		return x * x + z * z;
	}

	private static Vec3 bodyCenter(LivingEntity entity) {
		return entity.position().add(0.0, entity.getBbHeight() * 0.52, 0.0);
	}

	private static boolean finite(Vec3 vector) {
		return vector != null && Double.isFinite(vector.x)
				&& Double.isFinite(vector.y) && Double.isFinite(vector.z);
	}
}

package com.powers.power.abilities;

import com.powers.PowersBlocks;
import com.powers.fx.LightningStrikeFx;
import com.powers.mind.BodyProxyManager;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.power.state.EntityFreezeController;
import com.powers.power.state.MagicShieldManager;
import com.powers.power.state.PowerEntityState;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.LoadedChunks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Resolves loaded sky columns, protected bodies, and one finite conductive chain. */
final class LightningStrikeImpactResolver {
	private static final double SKY_HEIGHT = 28.0;
	private static final int MAX_COUNTER_CUES = 8;
	private static final int MAX_HIT_HISTORY = 64;

	/** One loaded sky-to-surface endpoint and its pre-body result. */
	record StrikeSite(Vec3 sky, Vec3 point, BlockPos support,
			LightningStrikeRules.Counterplay counterplay) {
	}

	/** One completed verdict with evidence for lifecycle and Veil effects. */
	record ImpactResult(Vec3 point, int affected,
			LightningStrikeRules.Counterplay counterplay) {
	}

	/** Direct-impact evidence needed to seed exactly one wet chain. */
	private record DamageResult(int affected, LivingEntity chainOrigin,
			Set<UUID> struck) {
	}

	private LightningStrikeImpactResolver() {
	}

	/** Locates the initial lawful endpoint without loading a chunk. */
	static StrikeSite initialSite(ServerLevel level, ServerPlayer caster, Vec3 requested) {
		return locate(level, caster, requested);
	}

	/** Returns the current loaded warning endpoint without mutating entities. */
	static StrikeSite previewSite(ServerLevel level, ServerPlayer caster, Vec3 requested) {
		return locate(level, caster, requested);
	}

	/** Refuses a cast aimed directly at a body protected before payment commits. */
	static LightningStrikeRules.Counterplay initialBodyCounter(ServerLevel level,
			ServerPlayer caster, LivingEntity target) {
		if (target == null) return LightningStrikeRules.Counterplay.STRIKE;
		return bodyCounter(level, caster, target, caster.getEyePosition(),
				level.getServer().getTickCount(), false);
	}

	/** Resolves one primary or Dominion verdict without harmful vanilla lightning. */
	static ImpactResult resolve(ServerLevel level, ServerPlayer caster,
			StormTribunal tribunal, Vec3 requested, LightningStrikeRules.Beat beat) {
		StrikeSite site = locate(level, caster, requested);
		if (site == null) {
			Vec3 point = finite(requested) ? requested : caster.position();
			LightningStrikeFx.terminal(level, point,
					LightningStrikeRules.Counterplay.UNLOADED);
			return new ImpactResult(point, 0, LightningStrikeRules.Counterplay.UNLOADED);
		}
		LightningStrikeRules.Counterplay medium = site.counterplay();
		if (!LightningStrikeRules.impactAllowed(medium)) {
			LightningStrikeFx.terminal(level, site.point(), medium);
			return new ImpactResult(site.point(), 0, medium);
		}

		double radius = LightningStrikeRules.impactRadius(
				tribunal.baseRadius, beat, tribunal.empoweredImpact, medium);
		float damage = (float) (tribunal.baseDamage
				* LightningStrikeRules.damageMultiplier(
						beat, tribunal.empoweredImpact, medium));
		DamageResult direct = damageTargets(level, caster, tribunal,
				site, radius, damage, beat);
		int affected = direct.affected();
		if (beat == LightningStrikeRules.Beat.PRIMARY && direct.chainOrigin() != null) {
			affected += conduct(level, caster, tribunal,
					direct.chainOrigin(), direct.struck());
		}
		spawnVisualLightning(level, site.point());
		LightningStrikeFx.impact(level, site.sky(), site.point(), radius,
				beat, medium, affected, tribunal.empoweredImpact);
		return new ImpactResult(site.point(), affected, medium);
	}

	/** Finds a roof catch or supporting surface, then classifies the vertical sky path. */
	private static StrikeSite locate(ServerLevel level, ServerPlayer caster, Vec3 requested) {
		if (!finite(requested)) return null;
		BlockPos column = BlockPos.containing(requested);
		if (!level.getWorldBorder().isWithinBounds(column)
				|| !LoadedChunks.contains(level, column)) return null;

		int requestedY = Math.max(level.getMinY(),
				Math.min(level.getMaxY() - 1, column.getY()));
		int highestBlockingY = level.getHeight(Heightmap.Types.MOTION_BLOCKING,
				column.getX(), column.getZ()) - 1;
		if (highestBlockingY < level.getMinY()
				|| highestBlockingY >= level.getMaxY()) return null;
		BlockPos support = new BlockPos(column.getX(), highestBlockingY, column.getZ());
		BlockState state = level.getBlockState(support);
		if (state.getCollisionShape(level, support).isEmpty()
				&& state.getFluidState().isEmpty()) return null;
		boolean roof = LightningStrikeRules.roofCatch(highestBlockingY, requestedY);

		double surface = 1.0;
		if (state.getFluidState().isEmpty()) {
			var shape = state.getCollisionShape(level, support);
			if (!shape.isEmpty()) surface = shape.bounds().maxY;
		}
		Vec3 point = new Vec3(requested.x,
				support.getY() + surface + 0.02, requested.z);
		double availableSky = level.getMaxY() - 0.1 - point.y;
		Vec3 sky = point.add(0.0, Math.max(0.0,
				Math.min(SKY_HEIGHT, availableSky)), 0.0);
		SpellFieldManager.RayWardHit ward = SpellFieldManager.firstHarmfulRayIntercept(
				level, caster.getUUID(), sky, point).orElse(null);
		boolean sanctuary = ward != null
				&& ward.counterplay() == VoidBeamRules.Counterplay.SANCTUARY;
		boolean kineticWard = ward != null
				&& ward.counterplay() == VoidBeamRules.Counterplay.KINETIC_WARD;
		if (ward != null) {
			point = ward.point();
			support = BlockPos.containing(point);
		}
		boolean amethyst = state.is(AmethystDampening.AMETHYST_BLOCKS)
				|| AmethystDampening.findPoweredWard(level, support).isPresent();
		LightningStrikeRules.Counterplay counterplay = LightningStrikeRules.environmentDecision(
				true, true, PowerProtection.isSafeZone(level, point), amethyst,
				sanctuary, kineticWard, state.is(PowersBlocks.DARKNESS),
				state.getFluidState().is(FluidTags.WATER),
				state.is(PowersBlocks.PURE_LIGHT), roof);
		return new StrikeSite(sky, point, support, counterplay);
	}

	/** Applies deterministic nearest-first body work and finds one legal wet source. */
	private static DamageResult damageTargets(ServerLevel level, ServerPlayer caster,
			StormTribunal tribunal, StrikeSite site, double radius,
			float centreDamage, LightningStrikeRules.Beat beat) {
		AABB bounds = AABB.ofSize(site.point().add(0.0, radius * 0.35, 0.0),
				radius * 2.0, radius * 2.3, radius * 2.0);
		List<LivingEntity> candidates = BoundedEntityCandidates.living(level, bounds,
				LightningStrikeRules.directCandidateLimit(),
				entity -> entity.isAlive() && entity != caster && !entity.isSpectator()
						&& bodyCenter(entity).distanceToSqr(site.point()) <= radius * radius,
				Comparator.comparingDouble((LivingEntity entity) ->
						bodyCenter(entity).distanceToSqr(site.point())).thenComparing(
						entity -> entity.getUUID().toString()));

		int limit = Math.min(LightningStrikeRules.targetLimit(
				tribunal.empoweredImpact, tribunal.ancientMastery), candidates.size());
		int affected = 0;
		int counterCues = 0;
		LivingEntity chainOrigin = null;
		Set<UUID> struck = new LinkedHashSet<>();
		struck.add(caster.getUUID());
		long now = level.getServer().getTickCount();
		for (int index = 0; index < limit; index++) {
			LivingEntity target = candidates.get(index);
			if (tribunal.hits.getOrDefault(target.getUUID(), 0) >= 2) continue;
			if (!lineClear(level, site.point(), target)) {
				if (counterCues++ < MAX_COUNTER_CUES) {
					LightningStrikeFx.terminal(level, bodyCenter(target),
							LightningStrikeRules.Counterplay.OBSTRUCTED);
				}
				continue;
			}
			LightningStrikeRules.Counterplay counter = bodyCounter(
					level, caster, target, site.point(), now, true);
			double distance = Math.sqrt(bodyCenter(target).distanceToSqr(site.point()));
			float damage = (float) (centreDamage
					* LightningStrikeRules.falloff(distance, radius));
			if (counter != LightningStrikeRules.Counterplay.STRIKE) {
				if (counter == LightningStrikeRules.Counterplay.FORCEFIELD && damage > 0.0F) {
					target.hurtServer(level, PowerDamage.source(caster), damage);
				}
				if (counterCues++ < MAX_COUNTER_CUES) {
					LightningStrikeFx.terminal(level, bodyCenter(target), counter);
				}
				continue;
			}
			boolean hurt = damage > 0.0F
					&& target.hurtServer(level, PowerDamage.source(caster), damage);
			if (!hurt) {
				if (counterCues++ < MAX_COUNTER_CUES) {
					LightningStrikeFx.terminal(level, bodyCenter(target),
							LightningStrikeRules.Counterplay.RESISTED);
				}
				continue;
			}
			recordHit(tribunal, target.getUUID());
			struck.add(target.getUUID());
			affected++;
			LightningStrikeRules.Counterplay secondary = LightningStrikeRules.secondaryDecision(
					true, BodyProxyManager.isProxy(target), EntityFreezeController.isFrozen(target));
			if (secondary != LightningStrikeRules.Counterplay.STRIKE) {
				if (counterCues++ < MAX_COUNTER_CUES) {
					LightningStrikeFx.terminal(level, bodyCenter(target), secondary);
				}
				continue;
			}
			if (tribunal.trueSight) reveal(level, target);
			if (beat == LightningStrikeRules.Beat.PRIMARY
					&& chainOrigin == null && target.isInWater() && target.isAlive()) {
				chainOrigin = target;
			}
		}
		return new DamageResult(affected, chainOrigin, struck);
	}

	/** Grows one unique nearest-neighbour chain and an optional one-node Communion fork. */
	private static int conduct(ServerLevel level, ServerPlayer caster,
			StormTribunal tribunal, LivingEntity origin, Set<UUID> struck) {
		LivingEntity current = origin;
		int affected = 0;
		double range = LightningStrikeRules.chainRange(tribunal.empoweredImpact);
		int limit = LightningStrikeRules.chainLimit(
				tribunal.empoweredImpact, tribunal.ancientMastery);
		for (int link = 0; link < limit; link++) {
			LivingEntity next = nearestWetCandidate(level, caster, current, struck, range);
			if (next == null) break;
			LightningStrikeRules.Counterplay counter = bodyCounter(
					level, caster, next, bodyCenter(current),
					level.getServer().getTickCount(), true);
			double distance = current.distanceTo(next);
			boolean loaded = LoadedChunks.contains(level, next.blockPosition());
			if (!lineClear(level, bodyCenter(current), next)) {
				LightningStrikeFx.terminal(level, bodyCenter(next),
						LightningStrikeRules.Counterplay.OBSTRUCTED);
				break;
			}
			if (!LightningStrikeRules.chainEligible(next.isInWater(), loaded,
					struck.contains(next.getUUID()), distance, range, counter)) {
				LightningStrikeFx.terminal(level, bodyCenter(next), counter);
				if (counter == LightningStrikeRules.Counterplay.FORCEFIELD) {
					float attempted = (float) (tribunal.baseDamage
							* LightningStrikeRules.chainDamageMultiplier(link));
					next.hurtServer(level, PowerDamage.source(caster), attempted);
				}
				break;
			}

			float damage = (float) (tribunal.baseDamage
					* LightningStrikeRules.chainDamageMultiplier(link));
			if (damage <= 0.0F
					|| !next.hurtServer(level, PowerDamage.source(caster), damage)) {
				LightningStrikeFx.terminal(level, bodyCenter(next),
						LightningStrikeRules.Counterplay.RESISTED);
				break;
			}
			struck.add(next.getUUID());
			recordHit(tribunal, next.getUUID());
			affected++;
			LightningStrikeFx.chain(level, bodyCenter(current), bodyCenter(next), link, false);
			LightningStrikeRules.Counterplay secondary = LightningStrikeRules.secondaryDecision(
					true, BodyProxyManager.isProxy(next), EntityFreezeController.isFrozen(next));
			if (secondary != LightningStrikeRules.Counterplay.STRIKE) {
				LightningStrikeFx.terminal(level, bodyCenter(next), secondary);
				break;
			}
			if (tribunal.trueSight) reveal(level, next);
			if (LightningStrikeRules.forkAllowed(tribunal.soulEcho, link)) {
				affected += fork(level, caster, tribunal, next, struck, range);
			}
			current = next;
		}
		return affected;
	}

	/** Resolves Communion's reduced branch independently without extending the main chain. */
	private static int fork(ServerLevel level, ServerPlayer caster,
			StormTribunal tribunal, LivingEntity origin, Set<UUID> struck, double range) {
		LivingEntity target = nearestWetCandidate(level, caster, origin, struck, range);
		if (target == null) return 0;
		LightningStrikeRules.Counterplay counter = bodyCounter(level, caster, target,
				bodyCenter(origin), level.getServer().getTickCount(), true);
		if (!lineClear(level, bodyCenter(origin), target)) {
			LightningStrikeFx.terminal(level, bodyCenter(target),
					LightningStrikeRules.Counterplay.OBSTRUCTED);
			return 0;
		}
		if (!LightningStrikeRules.chainEligible(target.isInWater(),
				LoadedChunks.contains(level, target.blockPosition()),
				struck.contains(target.getUUID()), origin.distanceTo(target), range, counter)) {
			LightningStrikeFx.terminal(level, bodyCenter(target), counter);
			if (counter == LightningStrikeRules.Counterplay.FORCEFIELD) {
				target.hurtServer(level, PowerDamage.source(caster),
						(float) (tribunal.baseDamage
								* LightningStrikeRules.forkDamageMultiplier()));
			}
			return 0;
		}
		float damage = (float) (tribunal.baseDamage
				* LightningStrikeRules.forkDamageMultiplier());
		if (damage <= 0.0F
				|| !target.hurtServer(level, PowerDamage.source(caster), damage)) {
			LightningStrikeFx.terminal(level, bodyCenter(target),
					LightningStrikeRules.Counterplay.RESISTED);
			return 0;
		}
		struck.add(target.getUUID());
		recordHit(tribunal, target.getUUID());
		LightningStrikeFx.chain(level, bodyCenter(origin), bodyCenter(target), 1, true);
		LightningStrikeRules.Counterplay secondary = LightningStrikeRules.secondaryDecision(
				true, BodyProxyManager.isProxy(target), EntityFreezeController.isFrozen(target));
		if (secondary != LightningStrikeRules.Counterplay.STRIKE) {
			LightningStrikeFx.terminal(level, bodyCenter(target), secondary);
		} else if (tribunal.trueSight) {
			reveal(level, target);
		}
		return 1;
	}

	/** Finds the nearest unique wet body without pre-skipping a protected chain blocker. */
	private static LivingEntity nearestWetCandidate(ServerLevel level, ServerPlayer caster,
			LivingEntity origin, Set<UUID> struck, double range) {
		List<LivingEntity> candidates = BoundedEntityCandidates.living(level,
				origin.getBoundingBox().inflate(range),
				LightningStrikeRules.chainCandidateLimit(),
				candidate -> candidate.isAlive() && candidate != caster
						&& !candidate.isSpectator() && candidate.isInWater()
						&& !struck.contains(candidate.getUUID())
						&& candidate.distanceToSqr(origin) <= range * range
						&& LoadedChunks.contains(level, candidate.blockPosition()),
				Comparator.comparingDouble((LivingEntity candidate) ->
						candidate.distanceToSqr(origin)).thenComparing(
						candidate -> candidate.getUUID().toString()));
		return candidates.isEmpty() ? null : candidates.getFirst();
	}

	/** Resolves body amethyst, crossed wards, Sanctuary, and personal shields. */
	private static LightningStrikeRules.Counterplay bodyCounter(ServerLevel level,
			ServerPlayer caster, LivingEntity target, Vec3 rayOrigin, long now,
			boolean includeForcefield) {
		SpellFieldManager.RayWardHit ward = SpellFieldManager.firstHarmfulRayIntercept(
				level, caster.getUUID(), rayOrigin, bodyCenter(target)).orElse(null);
		boolean sanctuary = SpellFieldManager.isSanctuaryProtected(level, target)
				|| ward != null && ward.counterplay() == VoidBeamRules.Counterplay.SANCTUARY;
		boolean kineticWard = ward != null
				&& ward.counterplay() == VoidBeamRules.Counterplay.KINETIC_WARD;
		boolean forcefield = includeForcefield && target instanceof ServerPlayer player
				&& MagicShieldManager.global().active(player.getUUID(), now);
		return LightningStrikeRules.bodyDecision(PowerProtection.mayHarm(caster, target),
				bodyAmethyst(level, target), sanctuary, kineticWard, forcefield);
	}

	/** Insight reveals only a body that accepted damage and can carry secondary magic. */
	private static void reveal(ServerLevel level, LivingEntity target) {
		boolean concealed = target.isInvisible();
		if (target instanceof ServerPlayer player) {
			concealed |= InvisibilityToggleAbility.reveal(player);
		}
		target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80,
				0, true, false, true));
		if (concealed) LightningStrikeFx.revelation(level, bodyCenter(target));
	}

	/** Records no more than two authored verdict hits per body and sixty-four identities. */
	private static void recordHit(StormTribunal tribunal, UUID target) {
		if (!tribunal.hits.containsKey(target)
				&& tribunal.hits.size() >= MAX_HIT_HISTORY) {
			UUID eldest = tribunal.hits.keySet().iterator().next();
			tribunal.hits.remove(eldest);
		}
		tribunal.hits.merge(target, 1, Integer::sum);
	}

	/** Extends carried/tagged and nearby powered amethyst to every living body. */
	private static boolean bodyAmethyst(ServerLevel level, LivingEntity target) {
		BlockPos feet = target.blockPosition();
		return AmethystDampening.isDampened(target)
				|| level.getBlockState(feet).is(AmethystDampening.AMETHYST_BLOCKS)
				|| level.getBlockState(feet.below()).is(AmethystDampening.AMETHYST_BLOCKS)
				|| AmethystDampening.findPoweredWard(level, feet).isPresent();
	}

	/** Prevents radial and chained electricity from crossing ordinary collision. */
	private static boolean lineClear(ServerLevel level, Vec3 impact, LivingEntity target) {
		Vec3 start = impact.add(0.0, 0.24, 0.0);
		HitResult hit = level.clip(new ClipContext(start, bodyCenter(target),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
		return hit.getType() == HitResult.Type.MISS;
	}

	/** Spawns only visual lightning and marks it as disposable power state. */
	private static void spawnVisualLightning(ServerLevel level, Vec3 point) {
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(
				level, EntitySpawnReason.TRIGGERED);
		if (bolt == null) return;
		bolt.setPos(point.x, point.y, point.z);
		bolt.setVisualOnly(true);
		PowerEntityState.markEphemeral(bolt);
		level.addFreshEntity(bolt);
	}

	private static Vec3 bodyCenter(LivingEntity entity) {
		return entity.position().add(0.0, entity.getBbHeight() * 0.52, 0.0);
	}

	private static boolean finite(Vec3 vector) {
		return vector != null && Double.isFinite(vector.x)
				&& Double.isFinite(vector.y) && Double.isFinite(vector.z);
	}
}

package com.powers.power.abilities;

import com.powers.PowerStatusEffects;
import com.powers.PowersBlocks;
import com.powers.fx.StarfallFx;
import com.powers.mind.BodyProxyManager;
import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.power.state.EntityFreezeController;
import com.powers.power.state.MagicShieldManager;
import com.powers.power.state.PowerEntityState;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.LoadedChunks;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Resolves sky surfaces, matter, wards, protected bodies, and pressure per strike. */
final class StarfallImpactResolver {
	private static final double SKY_HEIGHT = 32.0;
	private static final int MAX_HIT_HISTORY = 64;
	private static final int MAX_COUNTER_CUES = 6;
	private static final double REGULAR_PRESSURE = 0.78;
	private static final double CROWN_PRESSURE = 1.08;
	private static final double REGULAR_LIFT = 0.24;
	private static final double CROWN_LIFT = 0.38;

	/** One located impact and its pre-body environmental result. */
	record StrikeResult(Vec3 point, int affected, StarfallRules.Counterplay counterplay) {
	}

	/** One previewable sky-to-surface endpoint. */
	record StrikeSite(Vec3 sky, Vec3 point, BlockPos support,
			StarfallRules.Counterplay counterplay) {
	}

	private StarfallImpactResolver() {
	}

	/** Locates the initial field centre and its immediate protection terminal. */
	static StrikeSite initialSite(ServerLevel level, ServerPlayer caster, Vec3 requested) {
		return locate(level, caster, requested);
	}

	/** Returns the current telegraph endpoint without mutating bodies or terrain. */
	static Vec3 previewPoint(ServerLevel level, ServerPlayer caster, Vec3 requested) {
		StrikeSite site = locate(level, caster, requested);
		return site == null ? requested : site.point();
	}

	/** Resolves one regular, mirrored, or crown strike and returns its visible endpoint. */
	static StrikeResult resolve(ServerLevel level, ServerPlayer caster,
			AstralConvergence storm, Vec3 requested, int index,
			boolean crown, boolean echo) {
		StrikeSite site = locate(level, caster, requested);
		if (site == null) {
			StarfallFx.terminal(level, requested, StarfallRules.Counterplay.UNLOADED);
			return new StrikeResult(requested, 0, StarfallRules.Counterplay.UNLOADED);
		}
		StarfallRules.Counterplay medium = site.counterplay();
		if (terminal(medium)) {
			StarfallFx.terminal(level, site.point(), medium);
			return new StrikeResult(site.point(), 0, medium);
		}

		double radius = StarfallRules.impactRadius(
				storm.empoweredImpact, crown, medium);
		double damageScale = StarfallRules.damageMultiplier(
				index, crown, storm.empoweredImpact, medium);
		if (echo) damageScale *= StarfallRules.echoDamageMultiplier();
		float damage = (float) (storm.baseDamage * damageScale);
		int affected = damageTargets(level, caster, storm, site, radius, damage, crown);
		spawnVisualLightning(level, site.point());

		if (echo) {
			// The connecting constellation is emitted by the caller once both endpoints are known.
		} else if (crown) {
			StarfallFx.crown(level, site.sky(), site.point(), radius, affected);
		} else if (medium == StarfallRules.Counterplay.WATER) {
			StarfallFx.conduction(level, site.sky(), site.point(), radius, affected);
		} else {
			StarfallFx.strike(level, site.sky(), site.point(), radius,
					index, affected, storm.empoweredImpact);
		}
		if (medium == StarfallRules.Counterplay.PURE_LIGHT) {
			StarfallFx.resonance(level, site.point(), radius);
		}
		return new StrikeResult(site.point(), affected, medium);
	}

	/** Finds a loaded top surface and applies sky-path counterplay in priority order. */
	private static StrikeSite locate(ServerLevel level, ServerPlayer caster, Vec3 requested) {
		if (!finite(requested)) return null;
		BlockPos column = BlockPos.containing(requested);
		if (!level.getWorldBorder().isWithinBounds(column)
				|| !LoadedChunks.contains(level, column)) return null;

		BlockPos support = column;
		Vec3 point = requested;
		boolean roof = false;
		int requestedY = Math.max(level.getMinY(),
				Math.min(level.getMaxY() - 1, column.getY()));
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
				column.getX(), requestedY, column.getZ());
		for (int y = requestedY + 1; y < level.getMaxY(); y++) {
			cursor.setY(y);
			BlockState state = level.getBlockState(cursor);
			if (state.getCollisionShape(level, cursor).isEmpty()) continue;
			support = cursor.immutable();
			point = new Vec3(requested.x, y - 0.02, requested.z);
			roof = true;
			break;
		}
		if (!roof) {
			for (int y = requestedY; y >= level.getMinY(); y--) {
				cursor.setY(y);
				BlockState state = level.getBlockState(cursor);
				if (state.getCollisionShape(level, cursor).isEmpty()
						&& state.getFluidState().isEmpty()) continue;
				support = cursor.immutable();
				point = new Vec3(requested.x, y + 1.02, requested.z);
				break;
			}
		}

		Vec3 sky = point.add(0.0, Math.min(SKY_HEIGHT,
				Math.max(4.0, level.getMaxY() - 1.0 - point.y)), 0.0);
		SpellFieldManager.RayWardHit ward = SpellFieldManager.firstHarmfulRayIntercept(
				level, caster.getUUID(), sky, point).orElse(null);
		if (ward != null) {
			StarfallRules.Counterplay counterplay = ward.counterplay()
					== VoidBeamRules.Counterplay.SANCTUARY
					? StarfallRules.Counterplay.SANCTUARY
					: StarfallRules.Counterplay.KINETIC_WARD;
			return new StrikeSite(sky, ward.point(), BlockPos.containing(ward.point()), counterplay);
		}

		BlockState state = level.getBlockState(support);
		StarfallRules.Counterplay counterplay = StarfallRules.impactDecision(
				true, true, PowerProtection.isSafeZone(level, point),
				state.is(AmethystDampening.AMETHYST_BLOCKS)
						|| AmethystDampening.findPoweredWard(level, support).isPresent(),
				false, false, state.is(PowersBlocks.DARKNESS),
				state.getFluidState().is(FluidTags.WATER),
				state.is(PowersBlocks.PURE_LIGHT), roof);
		return new StrikeSite(sky, point, support, counterplay);
	}

	/** Applies nearest-first capped damage after per-body protection and repeat checks. */
	private static int damageTargets(ServerLevel level, ServerPlayer caster,
			AstralConvergence storm, StrikeSite site, double radius,
			float centreDamage, boolean crown) {
		AABB bounds = AABB.ofSize(site.point(), radius * 2.0,
				radius * 2.0, radius * 2.0);
		List<LivingEntity> candidates = BoundedEntityCandidates.living(level, bounds, 192,
				entity -> entity.isAlive() && entity != caster && !entity.isSpectator()
						&& entity.position().distanceToSqr(site.point()) <= radius * radius);
		candidates.sort(Comparator.comparingDouble((LivingEntity entity) ->
				entity.position().distanceToSqr(site.point())).thenComparing(
				entity -> entity.getUUID().toString()));

		int limit = Math.min(StarfallRules.targetLimit(storm.ancientMastery), candidates.size());
		int affected = 0;
		int counterCues = 0;
		long now = level.getServer().getTickCount();
		for (int candidate = 0; candidate < limit; candidate++) {
			LivingEntity target = candidates.get(candidate);
			AstralConvergence.HitRecord history = storm.hits.get(target.getUUID());
			long lastHit = history == null ? Long.MIN_VALUE : history.lastHit();
			int priorHits = history == null ? 0 : history.hits();
			if (!StarfallRules.hitAllowed(now, lastHit, priorHits, storm.ancientMastery)
					|| !lineClear(level, site.point(), target)) continue;

			StarfallRules.Counterplay counter = targetCounter(level, caster, target, now);
			double distance = Math.sqrt(target.position().distanceToSqr(site.point()));
			float damage = (float) (centreDamage * StarfallRules.falloff(distance, radius));
			if (counter != StarfallRules.Counterplay.STRIKE) {
				if (counter == StarfallRules.Counterplay.FORCEFIELD && damage > 0.0F) {
					target.hurtServer(level, PowerDamage.source(caster), damage);
					recordHit(storm, target.getUUID(), now, priorHits);
				}
				if (counterCues++ < MAX_COUNTER_CUES) {
					StarfallFx.terminal(level, bodyCenter(target), counter);
				}
				continue;
			}

			boolean hurt = damage > 0.0F
					&& target.hurtServer(level, PowerDamage.source(caster), damage);
			if (!hurt) {
				if (counterCues++ < MAX_COUNTER_CUES) {
					StarfallFx.terminal(level, bodyCenter(target), StarfallRules.Counterplay.RESISTED);
				}
				continue;
			}
			recordHit(storm, target.getUUID(), now, priorHits);
			affected++;
			if (storm.trueSight) reveal(level, target);
			if ((storm.empoweredImpact || crown) && target.isAlive()) {
				StarfallRules.Counterplay pressure = applyPressure(level, caster, target,
						site.point(), StarfallRules.falloff(distance, radius), crown);
				if (pressure != StarfallRules.Counterplay.STRIKE
						&& counterCues++ < MAX_COUNTER_CUES) {
					StarfallFx.terminal(level, bodyCenter(target), pressure);
				}
			}
		}
		return affected;
	}

	/** Resolves target-local protection independently from the strike material. */
	private static StarfallRules.Counterplay targetCounter(ServerLevel level,
			ServerPlayer caster, LivingEntity target, long now) {
		if (!PowerProtection.mayHarm(caster, target)) return StarfallRules.Counterplay.SAFE_ZONE;
		if (AmethystDampening.isDampened(target)) return StarfallRules.Counterplay.AMETHYST;
		if (SpellFieldManager.isSanctuaryProtected(level, target)) {
			return StarfallRules.Counterplay.SANCTUARY;
		}
		if (target instanceof ServerPlayer player
				&& MagicShieldManager.global().active(player.getUUID(), now)) {
			return StarfallRules.Counterplay.FORCEFIELD;
		}
		return StarfallRules.Counterplay.STRIKE;
	}

	/** Writes Might/Dominion pressure only after every movement protection check. */
	private static StarfallRules.Counterplay applyPressure(ServerLevel level, ServerPlayer caster,
			LivingEntity target, Vec3 center, double falloff, boolean crown) {
		Vec3 impulse = StarfallRules.pressureImpulse(center, target.position(),
				(crown ? CROWN_PRESSURE : REGULAR_PRESSURE) * falloff,
				crown ? CROWN_LIFT : REGULAR_LIFT);
		Vec3 velocity = target.getDeltaMovement().scale(0.35).add(impulse);
		boolean clearPath = !impulse.equals(Vec3.ZERO)
				&& level.noBlockCollision(target, target.getBoundingBox().move(velocity));
		boolean forcefield = target instanceof ServerPlayer player
				&& MagicShieldManager.global().active(player.getUUID(),
						level.getServer().getTickCount());
		StarfallRules.Counterplay decision = StarfallRules.pressureDecision(
				PowerProtection.mayForceMove(caster, target),
				AmethystDampening.isDampened(target), BodyProxyManager.isProxy(target),
				forcefield, SpellFieldManager.blocksForcedMovement(
						level, target, caster.getUUID()),
				EntityFreezeController.isFrozen(target), clearPath);
		if (decision != StarfallRules.Counterplay.STRIKE) return decision;
		target.setDeltaMovement(velocity);
		target.hurtMarked = true;
		target.fallDistance = 0.0F;
		return StarfallRules.Counterplay.STRIKE;
	}

	/** Insight names a successfully struck veiled body without leaking it before impact. */
	private static void reveal(ServerLevel level, LivingEntity target) {
		boolean concealed = target.isInvisible();
		if (target instanceof ServerPlayer player) {
			concealed |= InvisibilityToggleAbility.reveal(player);
		}
		target.addEffect(PowerStatusEffects.hidden(MobEffects.GLOWING, 80, 0, true, true));
		if (concealed) StarfallFx.revelation(level, bodyCenter(target));
	}

	private static void recordHit(AstralConvergence storm, UUID target,
			long now, int priorHits) {
		if (!storm.hits.containsKey(target) && storm.hits.size() >= MAX_HIT_HISTORY) {
			UUID eldest = storm.hits.keySet().iterator().next();
			storm.hits.remove(eldest);
		}
		storm.hits.put(target, new AstralConvergence.HitRecord(now, priorHits + 1));
	}

	private static boolean lineClear(ServerLevel level, Vec3 impact, LivingEntity target) {
		Vec3 start = impact.add(0.0, 0.28, 0.0);
		Vec3 end = bodyCenter(target);
		HitResult hit = level.clip(new ClipContext(start, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
		return hit.getType() == HitResult.Type.MISS;
	}

	private static void spawnVisualLightning(ServerLevel level, Vec3 point) {
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt == null) return;
		bolt.setPos(point.x, point.y, point.z);
		bolt.setVisualOnly(true);
		PowerEntityState.markEphemeral(bolt);
		level.addFreshEntity(bolt);
	}

	private static boolean terminal(StarfallRules.Counterplay counterplay) {
		return counterplay != StarfallRules.Counterplay.STRIKE
				&& counterplay != StarfallRules.Counterplay.WATER
				&& counterplay != StarfallRules.Counterplay.PURE_LIGHT;
	}

	private static Vec3 bodyCenter(LivingEntity entity) {
		return entity.position().add(0.0, entity.getBbHeight() * 0.52, 0.0);
	}

	private static boolean finite(Vec3 vector) {
		return vector != null && Double.isFinite(vector.x)
				&& Double.isFinite(vector.y) && Double.isFinite(vector.z);
	}
}

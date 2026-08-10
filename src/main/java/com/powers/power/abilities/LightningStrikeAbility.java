package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.fx.LightningStrikeFx;
import com.powers.magic.runtime.MagicPresenceHandle;
import com.powers.magic.runtime.MagicPresenceId;
import com.powers.magic.runtime.PhysicalMagicPresences;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.ServerCastLifecycle;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.MagicUseGate;
import com.powers.power.Power;
import com.powers.power.PowerTargeting;
import com.powers.power.state.EntityFreezeController;
import com.powers.progression.ScaledMagicValues;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.LoadedChunks;
import com.powers.util.BoundedEntityCandidates;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
/** Owns a fast Storm Tribunal from its warning compass through Dominion's crown. */
public final class LightningStrikeAbility extends Ability {
	private static final Identifier POWER_ID = PowersMod.id("lightning_strike");
	private static final Identifier ELEMENTAL_POWER_ID = PowersMod.id("elemental_blast");
	private static final double BASE_RANGE = 64.0;
	private static final double BASE_RADIUS = 2.75;
	private static final float BASE_DAMAGE = 8.0F;
	private static final int MAX_ACTIVE_TRIBUNALS = 32;
	private static final double TRACKING_LEASH = 10.0;
	private static final double TRACKING_STEP = 1.0;
	private static final double PROJECTILE_COLUMN_RADIUS = 2.25;
	private static final double PROJECTILE_COLUMN_HEIGHT = 24.0;
	private static final EntityTypeTest<Entity, Projectile> PROJECTILE_TYPE =
			EntityTypeTest.forClass(Projectile.class);
	private static final EntityTypeTest<Entity, Mob> MOB_TYPE =
			EntityTypeTest.forClass(Mob.class);
	private static final Map<UUID, StormTribunal> ACTIVE = new LinkedHashMap<>();

	public LightningStrikeAbility() {
		super(POWER_ID, Component.translatable("ability.powers.lightning_strike"),
				0, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		return activateFrom(player, data, POWER_ID);
	}

	@Override
	public void bindPhysicalPresence(ServerPlayer player, PlayerPowers.PlayerPowersData data,
			MagicPresenceId presenceId) {
		StormTribunal tribunal = ACTIVE.get(player.getUUID());
		if (tribunal == null || !(player.level() instanceof ServerLevel level)) return;
		PhysicalMagicPresences.bindExistingFixed(presenceId, level, tribunal.center,
				MagicPresenceHandle.Kind.IMPACT, tribunal.expiresAt);
	}

	/** Starts Elemental Blast's storm phase while retaining its exact lifecycle owner. */
	boolean activateFromElemental(ServerPlayer player,
			PlayerPowers.PlayerPowersData data) {
		return activateFrom(player, data, ELEMENTAL_POWER_ID);
	}

	/** Validates and captures one direct or delegated paid source. */
	private boolean activateFrom(ServerPlayer player,
			PlayerPowers.PlayerPowersData data, Identifier sourcePower) {
		if (!player.isAlive()) return false;
		ServerLevel level = (ServerLevel) player.level();
		if (ACTIVE.containsKey(player.getUUID())) {
			PowerMessages.send(player, "ability.powers.lightning_strike.active", 3);
			return false;
		}
		if (ACTIVE.size() >= MAX_ACTIVE_TRIBUNALS) {
			LightningStrikeFx.terminal(level, player.getEyePosition(),
					LightningStrikeRules.Counterplay.RESISTED);
			PowerMessages.send(player, "ability.powers.lightning_strike.blocked", 4);
			return false;
		}

		ScaledMagicValues profile = scaling(player);
		double range = Math.min(128.0, BASE_RANGE * profile.rangeMultiplier());
		HitResult hit = PowerTargeting.raycast(player, range);
		Vec3 requested = hit.getType() == HitResult.Type.MISS
				? player.getEyePosition().add(player.getLookAngle().scale(range))
				: hit.getLocation();
		if (!finite(requested) || !LoadedChunks.contains(
				level, net.minecraft.core.BlockPos.containing(requested))) {
			Vec3 point = finite(requested) ? requested : player.getEyePosition();
			LightningStrikeFx.terminal(level, point,
					LightningStrikeRules.Counterplay.UNLOADED);
			PowerMessages.send(player, "ability.powers.lightning_strike.blocked", 4);
			return false;
		}

		LivingEntity aimedTarget = hit instanceof EntityHitResult entityHit
				&& entityHit.getEntity() instanceof LivingEntity living ? living : null;
		LightningStrikeRules.Counterplay bodyCounter =
				LightningStrikeImpactResolver.initialBodyCounter(
						level, player, aimedTarget);
		if (bodyCounter != LightningStrikeRules.Counterplay.STRIKE) {
			Vec3 point = aimedTarget == null ? requested : bodyCenter(aimedTarget);
			LightningStrikeFx.terminal(level, point, bodyCounter);
			PowerMessages.send(player, "ability.powers.lightning_strike.blocked", 4);
			return false;
		}

		LightningStrikeImpactResolver.StrikeSite site =
				LightningStrikeImpactResolver.initialSite(level, player, requested);
		if (site == null || !LightningStrikeRules.impactAllowed(site.counterplay())) {
			LightningStrikeRules.Counterplay counter = site == null
					? LightningStrikeRules.Counterplay.UNLOADED : site.counterplay();
			LightningStrikeFx.terminal(level,
					site == null ? requested : site.point(), counter);
			PowerMessages.send(player, "ability.powers.lightning_strike.blocked", 4);
			return false;
		}

		Set<String> variants = profile.unlockedVariants();
		boolean empoweredImpact = variants.contains("empowered_impact");
		boolean secondStep = variants.contains("second_step");
		boolean trueSight = variants.contains("true_sight");
		boolean reflectiveWard = variants.contains("reflective_ward");
		boolean soulEcho = variants.contains("soul_echo");
		boolean afterimage = variants.contains("afterimage");
		boolean ancientMastery = variants.contains("ancient_mastery");
		long now = level.getServer().getTickCount();
		UUID tracked = secondStep && aimedTarget != null
				? aimedTarget.getUUID() : null;
		double radius = Math.max(2.25,
				Math.min(5.0, BASE_RADIUS * profile.rangeMultiplier()));
		StormTribunal tribunal = new StormTribunal(player.getUUID(), player.getId(), sourcePower,
				CastScalingContext.currentSource(),
				level.dimension(), now,
				now + LightningStrikeRules.finishAge(ancientMastery),
				site.point(), tracked, radius,
				(float) (BASE_DAMAGE * profile.potencyMultiplier()), empoweredImpact,
				secondStep, trueSight, reflectiveWard, soulEcho,
				afterimage, ancientMastery);
		ACTIVE.put(tribunal.owner, tribunal);
		LightningStrikeFx.open(level, site.sky(), site.point(), tribunal.baseRadius,
				tracked != null, tribunal.soulEcho, tribunal.ancientMastery);
		PowerMessages.send(player, "ability.powers.lightning_strike.cast", 4);
		return true;
	}

	/** Advances every owned tribunal exactly once from the common server tick. */
	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		Iterator<Map.Entry<UUID, StormTribunal>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			StormTribunal tribunal = iterator.next().getValue();
			ServerLevel level = server.getLevel(tribunal.dimension);
			Entity ownerEntity = level == null ? null : level.getEntity(tribunal.ownerEntityId);
			ServerPlayer owner = ownerEntity instanceof ServerPlayer candidate
					&& candidate.getUUID().equals(tribunal.owner) ? candidate : null;
			boolean sameDimension = owner != null
					&& owner.level().dimension().equals(tribunal.dimension);
			boolean dampened = owner != null && AmethystDampening.isDampened(owner);
			boolean frozen = MagicUseGate.timeLocked(owner);
			boolean ownsPower = owner != null && ServerCastLifecycle.mayContinue(
					owner, tribunal.castSource, ownsSource(owner, tribunal.sourcePower));
			boolean siteLoaded = level != null && finite(tribunal.center)
					&& level.getWorldBorder().isWithinBounds(
							net.minecraft.core.BlockPos.containing(tribunal.center))
					&& LoadedChunks.contains(level,
							net.minecraft.core.BlockPos.containing(tribunal.center));
			boolean continues = LightningStrikeRules.tribunalContinues(
					owner != null, sameDimension,
					owner != null && owner.isAlive() && !owner.isRemoved(),
					dampened, frozen, ownsPower, siteLoaded, now, tribunal.expiresAt);
			if (level == null || !continues) {
				boolean completed = level != null && owner != null && sameDimension
						&& owner.isAlive() && !dampened && !frozen && ownsPower && siteLoaded
						&& now >= tribunal.expiresAt;
				if (level != null) {
					LightningStrikeFx.close(level, tribunal.center,
							tribunal.baseRadius, completed, dampened, frozen);
				}
				if (completed && owner != null) {
					PowerMessages.send(owner,
							"ability.powers.lightning_strike.complete", 3);
				} else if (owner != null && sameDimension && owner.isAlive()) {
					PowerMessages.send(owner,
							"ability.powers.lightning_strike.interrupted", 3);
				}
				iterator.remove();
				continue;
			}

			int age = (int) Math.min(Integer.MAX_VALUE,
					Math.max(0L, now - tribunal.startedAt));
			if (!tribunal.primaryResolved) {
				updateTrackedCenter(level, tribunal, age);
				if (age < LightningStrikeRules.beatAge(
						LightningStrikeRules.Beat.PRIMARY)) {
					LightningStrikeImpactResolver.StrikeSite preview =
							LightningStrikeImpactResolver.previewSite(
									level, owner, tribunal.center);
					if (!lawful(preview)) {
						interruptAtSite(level, owner, tribunal, preview);
						iterator.remove();
						continue;
					}
					Vec3 point = preview.point();
					Vec3 sky = preview.sky();
					LightningStrikeFx.omen(level, sky, point,
							tribunal.baseRadius, age, tribunal.afterimage,
							tribunal.ancientMastery);
					if ((age & 1) == 0) groundProjectiles(level, tribunal, point, sky);
					telegraph(level, tribunal, preview, age,
							LightningStrikeRules.Beat.PRIMARY);
				}
			}

			if (!tribunal.primaryResolved && age >= LightningStrikeRules.beatAge(
					LightningStrikeRules.Beat.PRIMARY)) {
				tribunal.primaryResolved = true;
				LightningStrikeImpactResolver.ImpactResult result =
						LightningStrikeImpactResolver.resolve(level, owner, tribunal,
								tribunal.center, LightningStrikeRules.Beat.PRIMARY);
				if (!LightningStrikeRules.impactAllowed(result.counterplay())) {
					LightningStrikeFx.close(level, result.point(), tribunal.baseRadius,
							false, result.counterplay()
									== LightningStrikeRules.Counterplay.AMETHYST, false);
					PowerMessages.send(owner,
							"ability.powers.lightning_strike.interrupted", 3);
					iterator.remove();
					continue;
				}
				if (result.affected() > 0) applyAfterimage(level, owner, tribunal, result.point());
			}

			if (tribunal.ancientMastery && !tribunal.crownResolved) {
				int until = LightningStrikeRules.beatAge(
						LightningStrikeRules.Beat.CROWN) - age;
				if (until >= 1 && until <= 3) {
					LightningStrikeImpactResolver.StrikeSite preview =
							LightningStrikeImpactResolver.previewSite(
									level, owner, tribunal.center);
					if (!lawful(preview)) {
						interruptAtSite(level, owner, tribunal, preview);
						iterator.remove();
						continue;
					}
					telegraph(level, tribunal, preview, age,
							LightningStrikeRules.Beat.CROWN);
				}
				if (age >= LightningStrikeRules.beatAge(
						LightningStrikeRules.Beat.CROWN)) {
					tribunal.crownResolved = true;
					LightningStrikeImpactResolver.ImpactResult result =
							LightningStrikeImpactResolver.resolve(level, owner, tribunal,
							tribunal.center, LightningStrikeRules.Beat.CROWN);
					if (!LightningStrikeRules.impactAllowed(result.counterplay())) {
						LightningStrikeFx.close(level, result.point(), tribunal.baseRadius,
								false, result.counterplay()
										== LightningStrikeRules.Counterplay.AMETHYST, false);
						PowerMessages.send(owner,
								"ability.powers.lightning_strike.interrupted", 3);
						iterator.remove();
					}
				}
			}
		}
	}

	/** Removes one tribunal during respawn or disconnect. */
	public static void clear(MinecraftServer server, UUID owner) {
		StormTribunal tribunal = ACTIVE.remove(owner);
		if (tribunal == null || server == null) return;
		ServerLevel level = server.getLevel(tribunal.dimension);
		if (level != null) {
			LightningStrikeFx.close(level, tribunal.center,
					tribunal.baseRadius, false, false, false);
		}
	}

	/** Discards every tribunal before server world references are released. */
	public static void clearAll(MinecraftServer server) {
		if (server != null) {
			for (StormTribunal tribunal : new ArrayList<>(ACTIVE.values())) {
				ServerLevel level = server.getLevel(tribunal.dimension);
				if (level != null) {
					LightningStrikeFx.close(level, tribunal.center,
							tribunal.baseRadius, false, false, false);
				}
			}
		}
		ACTIVE.clear();
	}

	/** Motion tracks only the originally aimed body during the warning window. */
	private static void updateTrackedCenter(ServerLevel level,
			StormTribunal tribunal, int age) {
		if (!tribunal.secondStep || tribunal.trackedTarget == null) return;
		Entity entity = level.getEntity(tribunal.trackedTarget);
		if (!(entity instanceof LivingEntity target) || !target.isAlive()
				|| target.isSpectator()
				|| !LoadedChunks.contains(level, target.blockPosition())) return;
		Vec3 previous = tribunal.center;
		tribunal.center = LightningStrikeRules.trackedCenter(previous,
				target.position(), tribunal.origin, true,
				TRACKING_LEASH, TRACKING_STEP);
		LightningStrikeFx.tracking(level, previous, tribunal.center, age);
	}

	/** Telegraphs an unresolved primary or crown only during its final three ticks. */
	private static void telegraph(ServerLevel level, StormTribunal tribunal,
			LightningStrikeImpactResolver.StrikeSite preview, int age,
			LightningStrikeRules.Beat beat) {
		if ((beat == LightningStrikeRules.Beat.PRIMARY && tribunal.primaryResolved)
				|| (beat == LightningStrikeRules.Beat.CROWN
						&& tribunal.crownResolved)) return;
		int until = LightningStrikeRules.beatAge(beat) - age;
		if (until < 1 || until > 3) return;
		Vec3 point = preview == null ? tribunal.center : preview.point();
		Vec3 sky = preview == null ? point.add(0.0, 20.0, 0.0) : preview.sky();
		LightningStrikeFx.telegraph(level, sky, point, until, beat);
	}

	/** Returns whether dynamic column state still permits warning and rank effects. */
	private static boolean lawful(LightningStrikeImpactResolver.StrikeSite preview) {
		return preview != null && LightningStrikeRules.impactAllowed(preview.counterplay());
	}

	/** Closes a column whose environment became unlawful after payment committed. */
	private static void interruptAtSite(ServerLevel level, ServerPlayer owner,
			StormTribunal tribunal, LightningStrikeImpactResolver.StrikeSite preview) {
		LightningStrikeRules.Counterplay counter = preview == null
				? LightningStrikeRules.Counterplay.UNLOADED : preview.counterplay();
		Vec3 point = preview == null ? tribunal.center : preview.point();
		LightningStrikeFx.terminal(level, point, counter);
		LightningStrikeFx.close(level, point, tribunal.baseRadius,
				false, counter == LightningStrikeRules.Counterplay.AMETHYST, false);
		PowerMessages.send(owner, "ability.powers.lightning_strike.interrupted", 3);
	}

	/** Wardcraft grounds a finite hostile projectile set without owner transfer. */
	private static void groundProjectiles(ServerLevel level,
			StormTribunal tribunal, Vec3 point, Vec3 sky) {
		int limit = LightningStrikeRules.projectileLimit(tribunal.reflectiveWard);
		if (limit <= tribunal.groundedProjectiles.size()
				|| PowerProtection.isSafeZone(level, point)) return;
		double height = Math.min(PROJECTILE_COLUMN_HEIGHT,
				Math.max(4.0, sky.y - point.y));
		Vec3 center = point.add(0.0, height * 0.5, 0.0);
		AABB bounds = AABB.ofSize(center, PROJECTILE_COLUMN_RADIUS * 2.0,
				height + 2.0, PROJECTILE_COLUMN_RADIUS * 2.0);
		int scanLimit = LightningStrikeRules.rankCandidateLimit();
		List<Projectile> projectiles = BoundedEntityCandidates.collect(level,
				PROJECTILE_TYPE, bounds, scanLimit,
				projectile -> projectile.isAlive()
						&& !tribunal.groundedProjectiles.contains(projectile.getUUID())
						&& (projectile.getOwner() == null
								|| !projectile.getOwner().getUUID().equals(tribunal.owner))
						&& horizontalDistanceSquared(projectile.position(), point)
								<= PROJECTILE_COLUMN_RADIUS * PROJECTILE_COLUMN_RADIUS
						&& LoadedChunks.contains(level, projectile.blockPosition()),
				Comparator.comparingDouble((Projectile projectile) ->
						projectile.position().distanceToSqr(point)).thenComparing(
						projectile -> projectile.getUUID().toString()));
		for (Projectile projectile : projectiles) {
			if (tribunal.groundedProjectiles.size() >= limit) break;
			Vec3 velocity = LightningStrikeRules.groundedProjectileVelocity(
					projectile.getDeltaMovement(), 0.55);
			if (velocity.equals(Vec3.ZERO)) continue;
			projectile.setDeltaMovement(velocity);
			projectile.hurtMarked = true;
			tribunal.groundedProjectiles.add(projectile.getUUID());
			LightningStrikeFx.groundedProjectile(level, point,
					projectile.position(), tribunal.groundedProjectiles.size() - 1);
		}
	}

	/** Veil spends one successful false-storm ceremony and bounded aggro clear. */
	private static void applyAfterimage(ServerLevel level, ServerPlayer owner,
			StormTribunal tribunal, Vec3 point) {
		if (!tribunal.afterimage || tribunal.afterimageSpent) return;
		tribunal.afterimageSpent = true;
		int limit = LightningStrikeRules.afterimageTargetLimit(true);
		AABB bounds = AABB.ofSize(owner.position(), 20.0, 12.0, 20.0);
		int scanLimit = LightningStrikeRules.rankCandidateLimit();
		List<Mob> mobs = BoundedEntityCandidates.collect(level, MOB_TYPE, bounds,
				scanLimit, mob -> mob.isAlive() && mob.getTarget() == owner
						&& mob.hasLineOfSight(owner)
						&& !EntityFreezeController.isFrozen(mob)
						&& !PowerProtection.isSafeZone(level, mob.position())
						&& !SpellFieldManager.isSanctuaryProtected(level, mob),
				Comparator.comparingDouble((Mob mob) -> mob.distanceToSqr(owner))
						.thenComparing(mob -> mob.getUUID().toString()));
		int cleared = Math.min(limit, mobs.size());
		for (int index = 0; index < cleared; index++) mobs.get(index).setTarget(null);
		LightningStrikeFx.afterimage(level, point, cleared);
	}

	/** Requires the exact direct or delegated power captured when payment committed. */
	private static boolean ownsSource(ServerPlayer player, Identifier sourcePower) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		boolean lightningOwned = false;
		boolean elementalOwned = false;
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power == null) continue;
			lightningOwned |= power.id().equals(POWER_ID);
			elementalOwned |= power.id().equals(ELEMENTAL_POWER_ID);
		}
		return LightningStrikeRules.sourceOwned(sourcePower.equals(ELEMENTAL_POWER_ID),
				lightningOwned, elementalOwned);
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

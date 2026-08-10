package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.fx.StarfallFx;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.ServerCastLifecycle;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.MagicUseGate;
import com.powers.power.Power;
import com.powers.power.PowerTargeting;
import com.powers.power.state.EntityFreezeController;
import com.powers.power.state.PowerEntityState;
import com.powers.progression.ScaledMagicValues;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.LoadedChunks;
import com.powers.util.PowerMessages;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
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

/** Owns a staged Astral Convergence from its warning astrolabe through crown. */
public final class StarfallAbility extends Ability {
	private static final Identifier POWER_ID = PowersMod.id("starfall");
	private static final double BASE_TARGET_RANGE = 64.0;
	private static final double BASE_STORM_RADIUS = 6.0;
	private static final float BASE_DAMAGE = 6.0F;
	private static final int MAX_ACTIVE_STORMS = 32;
	private static final double TRACKING_LEASH = 16.0;
	private static final double TRACKING_STEP = 1.25;
	private static final double PROJECTILE_DIVERSION = 0.55;
	private static final double MAX_PROJECTILE_SPEED = 3.0;
	private static final Map<UUID, AstralConvergence> ACTIVE = new LinkedHashMap<>();

	public StarfallAbility() {
		super(POWER_ID, Component.translatable("ability.powers.starfall"), 300, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (!player.isAlive()) return false;
		if (ACTIVE.containsKey(player.getUUID())) {
			PowerMessages.send(player, "ability.powers.starfall.active", 3);
			return false;
		}
		if (ACTIVE.size() >= MAX_ACTIVE_STORMS) {
			StarfallFx.blocked((ServerLevel) player.level(), player.getEyePosition(),
					StarfallRules.Counterplay.RESISTED);
			PowerMessages.send(player, "ability.powers.starfall.blocked", 4);
			return false;
		}

		ServerLevel level = (ServerLevel) player.level();
		ScaledMagicValues profile = scaling(player);
		double range = Math.min(128.0, BASE_TARGET_RANGE * profile.rangeMultiplier());
		HitResult hit = PowerTargeting.raycast(player, range);
		Vec3 requested = hit.getType() == HitResult.Type.MISS
				? player.getEyePosition().add(player.getLookAngle().scale(range))
				: hit.getLocation();
		if (!finite(requested) || !LoadedChunks.contains(level,
				net.minecraft.core.BlockPos.containing(requested))) {
			Vec3 visualPoint = finite(requested) ? requested : player.getEyePosition();
			StarfallFx.blocked(level, visualPoint, StarfallRules.Counterplay.UNLOADED);
			PowerMessages.send(player, "ability.powers.starfall.blocked", 4);
			return false;
		}

		LivingEntity aimedTarget = hit instanceof EntityHitResult entityHit
				&& entityHit.getEntity() instanceof LivingEntity living ? living : null;
		StarfallRules.Counterplay bodyCounter = initialBodyCounter(level, player, aimedTarget);
		if (bodyCounter != StarfallRules.Counterplay.STRIKE) {
			Vec3 point = aimedTarget == null ? requested : bodyCenter(aimedTarget);
			StarfallFx.blocked(level, point, bodyCounter);
			PowerMessages.send(player, "ability.powers.starfall.blocked", 4);
			return false;
		}

		StarfallImpactResolver.StrikeSite site =
				StarfallImpactResolver.initialSite(level, player, requested);
		if (site == null || initialTerminal(site.counterplay())) {
			StarfallRules.Counterplay counter = site == null
					? StarfallRules.Counterplay.UNLOADED : site.counterplay();
			StarfallFx.blocked(level, site == null ? requested : site.point(), counter);
			PowerMessages.send(player, "ability.powers.starfall.blocked", 4);
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
		int strikes = StarfallRules.strikeCount(empoweredImpact, ancientMastery);
		long now = level.getServer().getTickCount();
		long expiry = now + StarfallRules.finishAge(strikes, ancientMastery);
		UUID tracked = secondStep && aimedTarget != null ? aimedTarget.getUUID() : null;
		double stormRadius = Math.max(4.0,
				Math.min(12.0, BASE_STORM_RADIUS * profile.rangeMultiplier()));
		long seed = player.getUUID().getMostSignificantBits()
				^ Long.rotateLeft(player.getUUID().getLeastSignificantBits(), 17) ^ now;
		AstralConvergence storm = new AstralConvergence(player.getUUID(), level.dimension(),
				CastScalingContext.currentSource(),
				now, expiry, seed, site.point(), tracked, stormRadius,
				(float) (BASE_DAMAGE * profile.potencyMultiplier()),
				CombatTerrainImpact.tier(player, CastScalingContext.currentSource()), strikes,
				empoweredImpact, secondStep, trueSight, reflectiveWard, soulEcho,
				afterimage, ancientMastery);
		ACTIVE.put(storm.owner, storm);
		StarfallFx.open(level, storm.center, storm.stormRadius, storm.strikeCount,
				tracked != null, storm.ancientMastery);
		PowerMessages.send(player, "ability.powers.starfall.cast", 4);
		return true;
	}

	/** Advances every owned storm exactly once from the common server tick. */
	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		Iterator<Map.Entry<UUID, AstralConvergence>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			AstralConvergence storm = iterator.next().getValue();
			ServerPlayer owner = server.getPlayerList().getPlayer(storm.owner);
			ServerLevel level = server.getLevel(storm.dimension);
			boolean sameDimension = owner != null
					&& owner.level().dimension().equals(storm.dimension);
			boolean dampened = owner != null && AmethystDampening.isDampened(owner);
			boolean frozen = MagicUseGate.timeLocked(owner);
			boolean ownsCast = owner != null && ServerCastLifecycle.mayContinue(
					owner, storm.castSource, ownsPower(owner));
			boolean continues = StarfallRules.stormContinues(owner != null, sameDimension,
					owner != null && owner.isAlive() && !owner.isRemoved(), dampened, frozen,
					ownsCast, now, storm.expiresAt);
			if (level == null || !continues) {
				boolean completed = level != null && owner != null && sameDimension
						&& owner.isAlive() && !dampened && !frozen
						&& ownsCast && now >= storm.expiresAt;
				if (level != null) {
					StarfallFx.collapse(level, storm.center, storm.stormRadius,
							completed, dampened, frozen);
				}
				if (completed && owner != null) {
					PowerMessages.send(owner, "ability.powers.starfall.complete", 3);
				} else if (owner != null && sameDimension && owner.isAlive()) {
					PowerMessages.send(owner, "ability.powers.starfall.interrupted", 3);
				}
				iterator.remove();
				continue;
			}

			int age = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, now - storm.startedAt));
			updateTrackedCenter(level, storm, age);
			if ((age & 1) == 0) divertProjectiles(level, storm, age);
			if (StarfallRules.phase(age, storm.strikeCount,
					storm.ancientMastery) == StarfallRules.Phase.OMEN) {
				if ((age & 1) == 0) {
					StarfallFx.omen(level, storm.center, storm.stormRadius,
							age, storm.afterimage, storm.ancientMastery);
				}
				telegraphNext(level, owner, storm, age);
				continue;
			}

			telegraphNext(level, owner, storm, age);
			int due = StarfallRules.strikesDue(age, storm.strikeCount);
			while (storm.nextStrike < due) {
				resolveRegular(level, owner, storm, storm.nextStrike++);
			}
			long crownAge = StarfallRules.crownAge(storm.strikeCount, storm.ancientMastery);
			if (!storm.crownResolved && storm.ancientMastery && age >= crownAge) {
				storm.crownResolved = true;
				StarfallImpactResolver.resolve(level, owner, storm,
						storm.center, storm.strikeCount, true, false);
			}
		}
	}

	/** Removes one storm during respawn or disconnect. */
	public static void clear(MinecraftServer server, UUID owner) {
		AstralConvergence storm = ACTIVE.remove(owner);
		if (storm == null || server == null) return;
		ServerLevel level = server.getLevel(storm.dimension);
		if (level != null) {
			StarfallFx.collapse(level, storm.center, storm.stormRadius,
					false, false, false);
		}
	}

	/** Discards every storm before server world state is released. */
	public static void clearAll(MinecraftServer server) {
		if (server != null) {
			for (AstralConvergence storm : new ArrayList<>(ACTIVE.values())) {
				ServerLevel level = server.getLevel(storm.dimension);
				if (level != null) {
					StarfallFx.collapse(level, storm.center, storm.stormRadius,
							false, false, false);
				}
			}
		}
		ACTIVE.clear();
	}

	/** Resolves one regular strike and its optional bounded Communion mirror. */
	private static void resolveRegular(ServerLevel level, ServerPlayer owner,
			AstralConvergence storm, int index) {
		Vec3 offset = StarfallRules.strikeOffset(
				storm.seed, index, storm.strikeCount, storm.stormRadius);
		StarfallImpactResolver.StrikeResult primary = StarfallImpactResolver.resolve(
				level, owner, storm, storm.center.add(offset), index, false, false);
		if (!StarfallRules.echoAllowed(storm.soulEcho, index)) return;
		Vec3 echoOffset = StarfallRules.echoOffset(offset);
		StarfallImpactResolver.StrikeResult echo = StarfallImpactResolver.resolve(
				level, owner, storm, storm.center.add(echoOffset), index, false, true);
		if (echo.counterplay() == StarfallRules.Counterplay.STRIKE
				|| echo.counterplay() == StarfallRules.Counterplay.WATER
				|| echo.counterplay() == StarfallRules.Counterplay.PURE_LIGHT) {
			StarfallFx.echo(level, primary.point(), echo.point(), index);
		}
	}

	/** Shows the next regular or crown endpoint for its final three warning ticks. */
	private static void telegraphNext(ServerLevel level, ServerPlayer owner,
			AstralConvergence storm, int age) {
		if (storm.nextStrike < storm.strikeCount) {
			int until = StarfallRules.strikeAge(storm.nextStrike) - age;
			if (until >= 1 && until <= 3) {
				Vec3 offset = StarfallRules.strikeOffset(storm.seed, storm.nextStrike,
						storm.strikeCount, storm.stormRadius);
				Vec3 point = StarfallImpactResolver.previewPoint(
						level, owner, storm.center.add(offset));
				StarfallFx.telegraph(level, point, until, storm.nextStrike, false);
			}
			return;
		}
		if (storm.ancientMastery && !storm.crownResolved) {
			long rawUntil = StarfallRules.crownAge(storm.strikeCount, true) - age;
			if (rawUntil >= 1 && rawUntil <= 3) {
				Vec3 point = StarfallImpactResolver.previewPoint(level, owner, storm.center);
				StarfallFx.telegraph(level, point, (int) rawUntil,
						storm.strikeCount, true);
			}
		}
	}

	/** Applies Motion tracking only while the original target remains legal and leashed. */
	private static void updateTrackedCenter(ServerLevel level,
			AstralConvergence storm, int age) {
		if (!storm.secondStep || storm.trackedTarget == null) return;
		Entity entity = level.getEntity(storm.trackedTarget);
		if (!(entity instanceof LivingEntity target) || !target.isAlive()
				|| target.isSpectator() || !LoadedChunks.contains(level, target.blockPosition())) return;
		Vec3 previous = storm.center;
		storm.center = StarfallRules.trackedCenter(previous, target.position(), storm.origin,
				true, TRACKING_LEASH, TRACKING_STEP);
		if ((age & 3) == 0) StarfallFx.tracking(level, previous, storm.center, age);
	}

	/** Wardcraft curves a finite hostile-projectile set without changing its owners. */
	private static void divertProjectiles(ServerLevel level,
			AstralConvergence storm, int age) {
		int limit = StarfallRules.projectileLimit(storm.reflectiveWard);
		if (limit <= 0 || PowerProtection.isSafeZone(level, storm.center)) return;
		AABB bounds = AABB.ofSize(storm.center.add(0.0, 2.0, 0.0),
				storm.stormRadius * 2.0, 8.0, storm.stormRadius * 2.0);
		List<Projectile> projectiles = BoundedEntityCandidates.ofClass(level, Projectile.class,
				bounds, 128,
				projectile -> projectile.isAlive()
						&& (projectile.getOwner() == null
								|| !projectile.getOwner().getUUID().equals(storm.owner))
						&& projectile.position().distanceToSqr(storm.center)
								<= storm.stormRadius * storm.stormRadius);
		projectiles.sort(Comparator.comparingDouble((Projectile projectile) ->
				projectile.position().distanceToSqr(storm.center)).thenComparing(
				projectile -> projectile.getUUID().toString()));
		int diverted = 0;
		for (Projectile projectile : projectiles) {
			if (diverted >= limit) break;
			Vec3 velocity = StarfallRules.divertProjectile(projectile.position(),
					projectile.getDeltaMovement(), storm.center,
					PROJECTILE_DIVERSION, MAX_PROJECTILE_SPEED);
			if (velocity.equals(Vec3.ZERO) || !PowerEntityState.tryReflect(projectile, 1)) continue;
			projectile.setDeltaMovement(velocity);
			projectile.hurtMarked = true;
			StarfallFx.wardProjectile(level, storm.center, projectile.position(), age, diverted++);
		}
	}

	/** Refuses a cast aimed directly at a body already protected from hostile magic. */
	private static StarfallRules.Counterplay initialBodyCounter(ServerLevel level,
			ServerPlayer caster, LivingEntity target) {
		if (target == null) return StarfallRules.Counterplay.STRIKE;
		if (!PowerProtection.mayHarm(caster, target)) return StarfallRules.Counterplay.SAFE_ZONE;
		if (AmethystDampening.isDampened(target)) return StarfallRules.Counterplay.AMETHYST;
		if (SpellFieldManager.isSanctuaryProtected(level, target)) {
			return StarfallRules.Counterplay.SANCTUARY;
		}
		return StarfallRules.Counterplay.STRIKE;
	}

	private static boolean initialTerminal(StarfallRules.Counterplay counterplay) {
		return counterplay == StarfallRules.Counterplay.UNOWNED
				|| counterplay == StarfallRules.Counterplay.UNLOADED
				|| counterplay == StarfallRules.Counterplay.SAFE_ZONE
				|| counterplay == StarfallRules.Counterplay.AMETHYST
				|| counterplay == StarfallRules.Counterplay.SANCTUARY
				|| counterplay == StarfallRules.Counterplay.KINETIC_WARD;
	}

	private static boolean ownsPower(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power != null && power.id().equals(POWER_ID)) return true;
		}
		return false;
	}

	private static Vec3 bodyCenter(LivingEntity entity) {
		return entity.position().add(0.0, entity.getBbHeight() * 0.52, 0.0);
	}

	private static boolean finite(Vec3 vector) {
		return vector != null && Double.isFinite(vector.x)
				&& Double.isFinite(vector.y) && Double.isFinite(vector.z);
	}
}

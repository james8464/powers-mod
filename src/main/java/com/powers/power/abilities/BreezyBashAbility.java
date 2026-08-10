package com.powers.power.abilities;

import com.powers.PowerStatusEffects;
import com.powers.PowersMod;
import com.powers.fx.BreezyBashFx;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.ServerCastLifecycle;
import com.powers.mind.BodyProxyManager;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.MagicUseGate;
import com.powers.power.Power;
import com.powers.power.state.EntityFreezeController;
import com.powers.power.state.MagicShieldManager;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.PowerMessages;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Owns a bounded launch-apex-slam Tempest Rite with complete movement counterplay. */
public final class BreezyBashAbility extends Ability {
	private static final Identifier POWER_ID = PowersMod.id("breezy_bash");
	private static final int RESOLUTION_DELAY_TICKS = 18;
	private static final int MAX_ACTIVE_RITES = 64;
	private static final int MAX_SCAN_CANDIDATES = 96;
	private static final int MAX_RESISTANCE_CUES = 6;
	private static final double BASE_RADIUS = 8.0;
	private static final double BASE_OUTWARD_STRENGTH = 0.42;
	private static final double BASE_VERTICAL_STRENGTH = 1.45;
	private static final double BASE_SLAM_STRENGTH = 2.50;
	private static final double EMPOWERED_SLAM_STRENGTH = 3.05;
	private static final double PROJECTILE_CURVE_STRENGTH = 0.42;
	private static final double MAX_PROJECTILE_SPEED = 2.6;
	private static final Map<UUID, TempestRite> ACTIVE = new LinkedHashMap<>();
	private static final Map<UUID, UUID> TARGET_OWNERS = new LinkedHashMap<>();

	public BreezyBashAbility() {
		super(POWER_ID, net.minecraft.network.chat.Component.translatable(
				"ability.powers.breezy_bash"), 400, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		UUID owner = player.getUUID();
		if (ACTIVE.containsKey(owner) || ACTIVE.size() >= MAX_ACTIVE_RITES) return false;
		ServerLevel level = (ServerLevel) player.level();
		long now = level.getServer().getTickCount();
		Set<String> variants = scaling(player).unlockedVariants();
		boolean empowered = variants.contains("empowered_impact");
		boolean ancient = variants.contains("ancient_mastery");
		double radius = Math.min(16.0, scaledRange(player, BASE_RADIUS));
		double force = Math.min(1.4, scaling(player).potencyMultiplier());
		Vec3 center = player.position();
		TempestRite rite = new TempestRite(owner, level.dimension(),
				CastScalingContext.currentSource(), center, now,
				now + RESOLUTION_DELAY_TICKS, radius,
				BreezyBashRules.targetLimit(empowered, ancient), empowered, ancient,
				BASE_OUTWARD_STRENGTH * force, BASE_VERTICAL_STRENGTH * force,
				empowered ? EMPOWERED_SLAM_STRENGTH * force : BASE_SLAM_STRENGTH * force,
				CombatTerrainImpact.tier(player, CastScalingContext.currentSource(), "breezy_bash"));

		captureInitialTargets(level, player, rite);
		if (rite.captured.isEmpty()) {
			BreezyBashFx.empty(level, center);
			PowerMessages.send(player, "ability.powers.breezy_bash.empty", 3);
			return false;
		}
		ACTIVE.put(owner, rite);
		BreezyBashFx.open(level, center, radius, empowered, ancient);
		PowerMessages.send(player, "ability.powers.breezy_bash.cast", 4);
		curveProjectiles(level, player, rite);
		return true;
	}

	/** Advances every active wind rite from the common authoritative server tick. */
	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		Iterator<Map.Entry<UUID, TempestRite>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			TempestRite rite = iterator.next().getValue();
			ServerPlayer owner = server.getPlayerList().getPlayer(rite.owner);
			ServerLevel level = server.getLevel(rite.dimension);
			if (!validOwner(owner, rite)) {
				if (level != null) releaseAll(level, rite, true);
				else releaseClaims(rite);
				iterator.remove();
				continue;
			}
			if (now >= rite.resolvesAt) {
				resolveSlam(level, owner, rite);
				iterator.remove();
				continue;
			}
			int age = (int) Math.max(0L, now - rite.startedAt);
			if ((age & 1) == 0) {
				revalidateCaptured(level, owner, rite);
				curveProjectiles(level, owner, rite);
			}
			if (age % 3 == 0) {
				BreezyBashFx.sustain(level, rite.center, rite.radius, age, rite.ancientMastery);
				emitTethers(level, rite);
			}
		}
	}

	/** Safely releases one owner's rite during respawn or disconnect. */
	public static void clear(MinecraftServer server, UUID owner) {
		TempestRite rite = ACTIVE.remove(owner);
		if (rite == null) return;
		ServerLevel level = server == null ? null : server.getLevel(rite.dimension);
		if (level != null) releaseAll(level, rite, true);
		else releaseClaims(rite);
	}

	/** Safely releases every rite before server world references are discarded. */
	public static void clearAll(MinecraftServer server) {
		for (TempestRite rite : new ArrayList<>(ACTIVE.values())) {
			ServerLevel level = server == null ? null : server.getLevel(rite.dimension);
			if (level != null) releaseAll(level, rite, true);
			else releaseClaims(rite);
		}
		ACTIVE.clear();
		TARGET_OWNERS.clear();
	}

	private static void captureInitialTargets(ServerLevel level,
			ServerPlayer owner, TempestRite rite) {
		AABB bounds = AABB.ofSize(rite.center, rite.radius * 2.0,
				rite.radius * 2.0, rite.radius * 2.0);
		List<LivingEntity> candidates = BoundedEntityCandidates.living(level, bounds,
				MAX_SCAN_CANDIDATES,
				entity -> entity.isAlive() && entity != owner && !entity.isSpectator()
						&& !entity.isPassenger()
						&& entity.position().distanceToSqr(rite.center) <= rite.radius * rite.radius);
		candidates.sort(Comparator.comparingDouble((LivingEntity entity) ->
				entity.position().distanceToSqr(rite.center)).thenComparing(
				entity -> entity.getUUID().toString()));

		int cues = 0;
		for (int index = 0; index < Math.min(MAX_SCAN_CANDIDATES, candidates.size()); index++) {
			if (rite.captured.size() >= rite.targetLimit) break;
			LivingEntity target = candidates.get(index);
			Vec3 impulse = BreezyBashRules.launchImpulse(
					rite.center, target.position(), rite.outwardStrength, rite.verticalStrength);
			Vec3 velocity = target.getDeltaMovement().scale(0.15).add(impulse);
			boolean clearPath = !velocity.equals(Vec3.ZERO)
					&& level.noBlockCollision(target, target.getBoundingBox().move(velocity));
			BreezyBashRules.CaptureDecision decision = captureDecision(
					level, owner, target, clearPath);
			if (decision == BreezyBashRules.CaptureDecision.CAPTURE) {
				TARGET_OWNERS.put(target.getUUID(), rite.owner);
				rite.captured.add(target.getUUID());
				target.setDeltaMovement(velocity);
				target.hurtMarked = true;
				target.fallDistance = 0.0F;
				BreezyBashFx.captured(level, rite.center,
						bodyCenter(target), rite.captured.size() - 1);
			} else if (cues < MAX_RESISTANCE_CUES) {
				BreezyBashFx.resistance(level, bodyCenter(target), decision);
				cues++;
			}
		}
	}

	private static void revalidateCaptured(ServerLevel level,
			ServerPlayer owner, TempestRite rite) {
		double maximumDistance = rite.radius + 8.0;
		Iterator<UUID> iterator = rite.captured.iterator();
		while (iterator.hasNext()) {
			UUID targetId = iterator.next();
			LivingEntity target = findTarget(level, targetId);
			if (target == null || !target.isAlive()) {
				releaseClaim(rite, targetId);
				iterator.remove();
				continue;
			}
			if (target.level() != level || target.position().distanceToSqr(rite.center)
					> maximumDistance * maximumDistance) {
				safeRelease(target);
				releaseClaim(rite, targetId);
				iterator.remove();
				continue;
			}
			BreezyBashRules.CaptureDecision decision = captureDecision(
					level, owner, target, true);
			if (decision == BreezyBashRules.CaptureDecision.CAPTURE) continue;
			if (decision == BreezyBashRules.CaptureDecision.TIME_LOCK) {
				EntityFreezeController.neutralizeReleaseMotion(target);
			}
			safeRelease(target);
			BreezyBashFx.resistance(level, bodyCenter(target), decision);
			releaseClaim(rite, targetId);
			iterator.remove();
		}
	}

	private static void resolveSlam(ServerLevel level,
			ServerPlayer owner, TempestRite rite) {
		int index = 0;
		for (UUID targetId : new ArrayList<>(rite.captured)) {
			LivingEntity target = findTarget(level, targetId);
			if (target == null || !target.isAlive()) {
				releaseClaim(rite, targetId);
				continue;
			}
			if (target.level() != level) {
				safeRelease(target);
				releaseClaim(rite, targetId);
				continue;
			}
			BreezyBashRules.CaptureDecision decision = captureDecision(
					level, owner, target, true);
			if (decision != BreezyBashRules.CaptureDecision.CAPTURE) {
				if (decision == BreezyBashRules.CaptureDecision.TIME_LOCK) {
					EntityFreezeController.neutralizeReleaseMotion(target);
				}
				safeRelease(target);
				BreezyBashFx.resistance(level, bodyCenter(target), decision);
				releaseClaim(rite, targetId);
				continue;
			}
			Vec3 slam = BreezyBashRules.slamVelocity(
					target.getDeltaMovement(), rite.slamStrength);
			target.setDeltaMovement(slam);
			target.hurtMarked = true;
			target.fallDistance = 0.0F;
			BreezyBashFx.slam(level, bodyCenter(target), rite.empoweredImpact, index++);
			releaseClaim(rite, targetId);
		}
		rite.captured.clear();
		CombatTerrainImpact.crater(level, owner, rite.center, rite.terrainTier);
		if (rite.empoweredImpact) BreezyBashFx.pressure(level, rite.center, rite.radius);
		BreezyBashFx.close(level, rite.center, rite.radius, false);
	}

	private static void curveProjectiles(ServerLevel level,
			ServerPlayer owner, TempestRite rite) {
		int limit = BreezyBashRules.projectileLimit(rite.ancientMastery);
		if (limit <= 0 || rite.curvedProjectiles.size() >= limit) return;
		AABB bounds = AABB.ofSize(rite.center, rite.radius * 2.0,
				rite.radius * 2.0, rite.radius * 2.0);
		List<Projectile> projectiles = BoundedEntityCandidates.ofClass(level, Projectile.class,
				bounds, 128,
				projectile -> projectile.isAlive()
						&& !rite.curvedProjectiles.contains(projectile.getUUID())
						&& (projectile.getOwner() == null
								|| !projectile.getOwner().getUUID().equals(rite.owner))
						&& !PowerProtection.isSafeZone(level, projectile.position())
						&& projectile.position().distanceToSqr(rite.center)
								<= rite.radius * rite.radius);
		projectiles.sort(Comparator.comparingDouble(
				projectile -> projectile.position().distanceToSqr(rite.center)));
		for (Projectile projectile : projectiles) {
			if (rite.curvedProjectiles.size() >= limit) break;
			Vec3 curved = BreezyBashRules.curveProjectile(projectile.position(),
					projectile.getDeltaMovement(), rite.center,
					PROJECTILE_CURVE_STRENGTH, MAX_PROJECTILE_SPEED);
			if (curved.equals(Vec3.ZERO)
					|| !level.noBlockCollision(projectile,
							projectile.getBoundingBox().move(curved))) continue;
			projectile.setDeltaMovement(curved);
			projectile.hurtMarked = true;
			rite.curvedProjectiles.add(projectile.getUUID());
			BreezyBashFx.projectileCurve(
					level, projectile.position(), curved, rite.curvedProjectiles.size() - 1);
		}
	}

	private static BreezyBashRules.CaptureDecision captureDecision(
			ServerLevel level, ServerPlayer owner, LivingEntity target, boolean clearPath) {
		boolean forcefield = MagicShieldManager.global().active(
				target.getUUID(), level.getServer().getTickCount());
		return BreezyBashRules.captureDecision(
				PowerProtection.mayForceMove(owner, target),
				AmethystDampening.isDampened(target), BodyProxyManager.isProxy(target),
				forcefield, SpellFieldManager.blocksForcedMovement(level, target, owner.getUUID()),
				EntityFreezeController.isFrozen(target), clearPath,
				BreezyBashRules.claimAllowed(TARGET_OWNERS.get(target.getUUID()), owner.getUUID()));
	}

	private static boolean validOwner(ServerPlayer owner, TempestRite rite) {
		boolean sameDimension = owner != null && owner.level().dimension().equals(rite.dimension);
		return BreezyBashRules.ownerValid(owner != null, sameDimension,
				owner != null && owner.isAlive(), owner != null && AmethystDampening.isDampened(owner),
				MagicUseGate.timeLocked(owner),
				owner != null && ServerCastLifecycle.mayContinue(
						owner, rite.castSource, ownsPower(owner)));
	}

	private static boolean ownsPower(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power != null && power.id().equals(POWER_ID)) return true;
		}
		return false;
	}

	private static void emitTethers(ServerLevel level, TempestRite rite) {
		int index = 0;
		for (UUID targetId : rite.captured) {
			if (index >= 12) return;
			Entity entity = level.getEntity(targetId);
			if (!(entity instanceof LivingEntity target)) continue;
			BreezyBashFx.tether(level, rite.center, bodyCenter(target), index++);
		}
	}

	private static void releaseAll(ServerLevel level, TempestRite rite, boolean interrupted) {
		for (UUID targetId : new ArrayList<>(rite.captured)) {
			LivingEntity target = findTarget(level, targetId);
			if (target != null && target.isAlive()) {
				if (EntityFreezeController.isFrozen(target)) {
					EntityFreezeController.neutralizeReleaseMotion(target);
				}
				safeRelease(target);
			}
			releaseClaim(rite, targetId);
		}
		rite.captured.clear();
		BreezyBashFx.close(level, rite.center, rite.radius, interrupted);
	}

	private static void safeRelease(LivingEntity target) {
		target.fallDistance = 0.0F;
		target.addEffect(PowerStatusEffects.hidden(MobEffects.SLOW_FALLING,
				60, 0, true, true));
		if (target.level() instanceof ServerLevel level) {
			BreezyBashFx.released(level, bodyCenter(target));
		}
	}

	private static LivingEntity findTarget(ServerLevel originalLevel, UUID targetId) {
		Entity entity = originalLevel.getEntity(targetId);
		if (entity instanceof LivingEntity living) return living;
		return originalLevel.getServer().getPlayerList().getPlayer(targetId);
	}

	private static void releaseClaim(TempestRite rite, UUID target) {
		TARGET_OWNERS.remove(target, rite.owner);
	}

	private static void releaseClaims(TempestRite rite) {
		TARGET_OWNERS.entrySet().removeIf(entry -> entry.getValue().equals(rite.owner));
		rite.captured.clear();
	}

	private static Vec3 bodyCenter(LivingEntity target) {
		return target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
	}

	/** Mutable rite state is private and accessed only from the server thread. */
	private static final class TempestRite {
		private final UUID owner;
		private final ResourceKey<Level> dimension;
		private final CastSource castSource;
		private final Vec3 center;
		private final long startedAt;
		private final long resolvesAt;
		private final double radius;
		private final int targetLimit;
		private final boolean empoweredImpact;
		private final boolean ancientMastery;
		private final double outwardStrength;
		private final double verticalStrength;
		private final double slamStrength;
		private final int terrainTier;
		private final Set<UUID> captured = new LinkedHashSet<>();
		private final Set<UUID> curvedProjectiles = new LinkedHashSet<>();

		private TempestRite(UUID owner, ResourceKey<Level> dimension, CastSource castSource, Vec3 center,
				long startedAt, long resolvesAt, double radius, int targetLimit,
				boolean empoweredImpact, boolean ancientMastery, double outwardStrength,
				double verticalStrength, double slamStrength, int terrainTier) {
			this.owner = owner;
			this.dimension = dimension;
			this.castSource = castSource;
			this.center = center;
			this.startedAt = startedAt;
			this.resolvesAt = resolvesAt;
			this.radius = radius;
			this.targetLimit = targetLimit;
			this.empoweredImpact = empoweredImpact;
			this.ancientMastery = ancientMastery;
			this.outwardStrength = outwardStrength;
			this.verticalStrength = verticalStrength;
			this.slamStrength = slamStrength;
			this.terrainTier = terrainTier;
		}
	}
}

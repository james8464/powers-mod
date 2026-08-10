package com.powers.power.abilities;

import com.powers.PowerStatusEffects;
import com.powers.PowersMod;
import com.powers.fx.GravityFx;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.ServerCastLifecycle;
import com.powers.mind.BodyProxyManager;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.MagicUseGate;
import com.powers.power.Power;
import com.powers.power.PowerDamage;
import com.powers.power.state.EntityFreezeController;
import com.powers.power.state.MagicShieldManager;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.PowerMessages;
import com.powers.util.BoundedEntityCandidates;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Anchors a bounded gravitational orrery that captures permitted targets,
 * bends mastered projectiles, and releases every body safely on all exits.
 */
public final class GravityDisplacementAbility extends Ability {
	private static final net.minecraft.resources.Identifier POWER_ID =
			PowersMod.id("gravity_displacement");
	private static final int BASE_DURATION_TICKS = 100;
	private static final double BASE_RADIUS = 8.0;
	private static final int SCAN_INTERVAL_TICKS = 2;
	private static final int VISUAL_INTERVAL_TICKS = 5;
	private static final int MAX_SCAN_CANDIDATES = 96;
	private static final int MAX_ACTIVE_FIELDS = 64;
	private static final int RELEASE_SLOW_FALL_TICKS = 60;
	private static final int RESISTANCE_CUE_INTERVAL_TICKS = 20;
	private static final int MAX_RESISTANCE_CUES_PER_SCAN = 4;
	private static final double STEERING_PULL = 0.18;
	private static final double MAX_ORBIT_SPEED = 0.85;
	private static final double PROJECTILE_BEND = 0.22;
	private static final double MAX_PROJECTILE_SPEED = 3.0;
	private static final double CLAIM_HYSTERESIS_SQUARED = 0.25;
	private static final Map<UUID, GravityField> ACTIVE = new LinkedHashMap<>();
	private static final Map<UUID, UUID> TARGET_OWNERS = new HashMap<>();

	public GravityDisplacementAbility() {
		super(PowersMod.id("gravity_displacement"),
				net.minecraft.network.chat.Component.translatable("ability.powers.gravity_displacement"),
				300, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		UUID owner = player.getUUID();
		if (ACTIVE.containsKey(owner) || ACTIVE.size() >= MAX_ACTIVE_FIELDS) return false;

		ServerLevel level = (ServerLevel) player.level();
		long now = level.getServer().getTickCount();
		Set<String> variants = scaling(player).unlockedVariants();
		boolean empoweredImpact = variants.contains("empowered_impact");
		boolean ancientMastery = variants.contains("ancient_mastery");
		double radius = scaledRange(player, BASE_RADIUS);
		int duration = scaledDuration(player, BASE_DURATION_TICKS);
		GravityField field = new GravityField(owner, level.dimension(),
				CastScalingContext.currentSource(), player.position(), now,
				now + duration, radius,
				GravityDisplacementRules.targetLimit(empoweredImpact, ancientMastery),
				empoweredImpact, ancientMastery, scaledPotency(player, 4.0F),
				Math.min(1.5, 1.05 * scaling(player).potencyMultiplier()));
		ACTIVE.put(owner, field);

		player.addEffect(PowerStatusEffects.hidden(MobEffects.SLOW_FALLING,
				duration + RELEASE_SLOW_FALL_TICKS, 0, true, true));
		GravityFx.open(level, field.center, field.radius, ancientMastery);
		PowerMessages.send(player, "ability.powers.gravity_displacement.cast", 4);
		refreshTargets(level, player, field, now);
		steerCaptured(level, field, now);
		return true;
	}

	/** Advances every field from the authoritative end-server-tick callback. */
	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		Iterator<Map.Entry<UUID, GravityField>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			GravityField field = iterator.next().getValue();
			ServerPlayer owner = server.getPlayerList().getPlayer(field.owner);
			ServerLevel level = server.getLevel(field.dimension);
			boolean sameDimension = owner != null && owner.level().dimension().equals(field.dimension);
			boolean dampened = owner != null && AmethystDampening.isDampened(owner);
			boolean frozen = MagicUseGate.timeLocked(owner);
			boolean ownsCast = owner != null && ServerCastLifecycle.mayContinue(
					owner, field.castSource, ownsPower(owner));
			if (level == null || !GravityDisplacementRules.fieldContinues(owner != null,
					sameDimension, owner != null && owner.isAlive(), dampened, frozen,
					ownsCast, now, field.expiresAt)) {
				boolean naturalExpiry = level != null && owner != null && sameDimension && owner.isAlive()
						&& !dampened && !frozen && ownsCast && now >= field.expiresAt;
				if (level != null) collapse(level, owner, field, naturalExpiry, !naturalExpiry);
				iterator.remove();
				continue;
			}

			if (now % SCAN_INTERVAL_TICKS == 0) {
				refreshTargets(level, owner, field, now);
				bendProjectiles(level, field, now);
			}
			steerCaptured(level, field, now);
			if (now % VISUAL_INTERVAL_TICKS == 0) {
				GravityFx.sustain(level, field.center, field.radius,
						(int) Math.max(0L, now - field.openedAt), field.ancientMastery);
				emitTethers(level, field);
			}
		}
	}

	/** Releases one owner's field during respawn or disconnect. */
	public static void clear(MinecraftServer server, UUID owner) {
		GravityField field = ACTIVE.remove(owner);
		if (field == null || server == null) return;
		ServerLevel level = server.getLevel(field.dimension);
		if (level != null) collapse(level, null, field, false, true);
	}

	/** Releases every capture before server world state is discarded. */
	public static void clearAll(MinecraftServer server) {
		if (server != null) {
			for (GravityField field : new ArrayList<>(ACTIVE.values())) {
				ServerLevel level = server.getLevel(field.dimension);
				if (level != null) collapse(level, null, field, false, true);
			}
		}
		ACTIVE.clear();
		TARGET_OWNERS.clear();
	}

	private static void refreshTargets(ServerLevel level,
			ServerPlayer owner, GravityField field, long now) {
		AABB bounds = AABB.ofSize(field.center, field.radius * 2.0,
				field.radius * 2.0, field.radius * 2.0);
		List<LivingEntity> candidates = BoundedEntityCandidates.living(level, bounds,
				MAX_SCAN_CANDIDATES,
				entity -> entity.isAlive() && entity != owner && !entity.isPassenger()
						&& (!(entity instanceof ServerPlayer player) || !player.isSpectator())
						&& entity.position().distanceToSqr(field.center) <= field.radius * field.radius);
		candidates.sort(Comparator
				.comparing((LivingEntity entity) -> !field.captured.contains(entity.getUUID()))
				.thenComparingDouble(entity -> entity.position().distanceToSqr(field.center))
				.thenComparing(entity -> entity.getUUID().toString()));

		Set<UUID> retained = new LinkedHashSet<>();
		int cues = 0;
		for (int index = 0; index < Math.min(MAX_SCAN_CANDIDATES, candidates.size()); index++) {
			LivingEntity target = candidates.get(index);
			GravityDisplacementRules.CaptureDecision decision = captureDecision(
					level, owner, target, now);
			if (decision == GravityDisplacementRules.CaptureDecision.CAPTURE
					&& retained.size() < field.targetLimit) {
				if (claimTarget(level, target, field)) {
					retained.add(target.getUUID());
					if (field.captured.add(target.getUUID())) {
						GravityFx.captured(level, field.center,
								target.position().add(0.0, target.getBbHeight() * 0.5, 0.0),
								field.ancientMastery);
					}
					continue;
				}
				decision = GravityDisplacementRules.CaptureDecision.GRAVITY_RESONANCE;
			}

			if (field.captured.remove(target.getUUID())) {
				if (decision == GravityDisplacementRules.CaptureDecision.TIME_LOCK) {
					EntityFreezeController.neutralizeReleaseMotion(target);
				}
				releaseClaim(field, target.getUUID());
				safeRelease(target);
			}
			if (decision != GravityDisplacementRules.CaptureDecision.CAPTURE
					&& cues < MAX_RESISTANCE_CUES_PER_SCAN && shouldCue(field, target.getUUID(), now)) {
				GravityFx.resistance(level,
						target.position().add(0.0, target.getBbHeight() * 0.5, 0.0), decision);
				cues++;
			}
		}

		for (UUID previous : new ArrayList<>(field.captured)) {
			if (retained.contains(previous)) continue;
			Entity entity = level.getEntity(previous);
			if (entity instanceof LivingEntity living) safeRelease(living);
			field.captured.remove(previous);
			releaseClaim(field, previous);
		}
		field.resistanceCues.entrySet().removeIf(entry -> now - entry.getValue() > 100L);
	}

	private static GravityDisplacementRules.CaptureDecision captureDecision(
			ServerLevel level, ServerPlayer owner, LivingEntity target, long now) {
		boolean movementAllowed = PowerProtection.mayForceMove(owner, target);
		boolean dampened = AmethystDampening.isDampened(target);
		boolean anchoredBody = BodyProxyManager.isProxy(target);
		boolean forcefield = MagicShieldManager.global().active(target.getUUID(), now);
		boolean spellWard = SpellFieldManager.blocksForcedMovement(
				level, target, owner.getUUID());
		boolean frozen = EntityFreezeController.isFrozen(target);
		return GravityDisplacementRules.captureDecision(
				movementAllowed, dampened, anchoredBody, forcefield, spellWard, frozen);
	}

	private static void steerCaptured(ServerLevel level, GravityField field, long now) {
		int age = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, now - field.openedAt));
		Iterator<UUID> iterator = field.captured.iterator();
		while (iterator.hasNext()) {
			UUID id = iterator.next();
			Entity entity = level.getEntity(id);
			boolean frozen = entity instanceof LivingEntity living
					&& EntityFreezeController.isFrozen(living);
			if (!(entity instanceof LivingEntity target) || !target.isAlive()
					|| target.position().distanceToSqr(field.center)
					> (field.radius + 2.0) * (field.radius + 2.0)
					|| frozen) {
				if (entity instanceof LivingEntity living) {
					if (frozen) EntityFreezeController.neutralizeReleaseMotion(living);
					safeRelease(living);
				}
				releaseClaim(field, id);
				iterator.remove();
				continue;
			}

			long seed = id.getMostSignificantBits() ^ Long.rotateLeft(id.getLeastSignificantBits(), 17);
			Vec3 desired = field.center.add(GravityDisplacementRules.orbitOffset(
					seed, age, field.radius, Math.min(4.5, field.radius * 0.56)));
			Vec3 velocity = GravityDisplacementRules.steeringVelocity(
					target.position(), target.getDeltaMovement(), desired, STEERING_PULL, MAX_ORBIT_SPEED);
			if (!level.noBlockCollision(target, target.getBoundingBox().move(velocity))) {
				velocity = new Vec3(velocity.x * 0.15, Math.max(0.12, velocity.y), velocity.z * 0.15);
				if (!level.noBlockCollision(target, target.getBoundingBox().move(velocity))) {
					velocity = Vec3.ZERO;
				}
			}
			target.setDeltaMovement(velocity);
			target.hurtMarked = true;
			target.fallDistance = 0.0F;
			target.addEffect(PowerStatusEffects.hidden(MobEffects.SLOW_FALLING,
					12, 0, true, true));
		}
	}

	private static void bendProjectiles(ServerLevel level, GravityField field, long now) {
		int limit = GravityDisplacementRules.projectileLimit(field.ancientMastery);
		if (limit <= 0) return;
		AABB bounds = AABB.ofSize(field.center, field.radius * 2.0,
				field.radius * 2.0, field.radius * 2.0);
		List<Projectile> projectiles = BoundedEntityCandidates.ofClass(level, Projectile.class,
				bounds, 128,
				projectile -> projectile.isAlive()
						&& projectile.position().distanceToSqr(field.center) <= field.radius * field.radius
						&& (projectile.getOwner() == null
								|| !projectile.getOwner().getUUID().equals(field.owner))
						&& !PowerProtection.isSafeZone(level, projectile.position()));
		projectiles.sort(Comparator.comparingDouble(
				projectile -> projectile.position().distanceToSqr(field.center)));
		for (int index = 0; index < Math.min(limit, projectiles.size()); index++) {
			Projectile projectile = projectiles.get(index);
			Vec3 bent = GravityDisplacementRules.bendProjectile(projectile.position(),
					projectile.getDeltaMovement(), field.center, PROJECTILE_BEND, MAX_PROJECTILE_SPEED);
			if (bent.equals(Vec3.ZERO)) continue;
			projectile.setDeltaMovement(bent);
			projectile.hurtMarked = true;
			if (index < 4) GravityFx.projectileCurve(level, projectile.position(), bent, now);
		}
	}

	private static void emitTethers(ServerLevel level, GravityField field) {
		int emitted = 0;
		for (UUID id : field.captured) {
			if (emitted >= 12) return;
			Entity entity = level.getEntity(id);
			if (!(entity instanceof LivingEntity target)) continue;
			GravityFx.tether(level, field.center.add(0.0, 1.0, 0.0),
					target.position().add(0.0, target.getBbHeight() * 0.5, 0.0),
					field.ancientMastery, emitted);
			emitted++;
		}
	}

	private static void collapse(ServerLevel level, ServerPlayer owner, GravityField field,
			boolean naturalExpiry, boolean interrupted) {
		boolean empowered = naturalExpiry && field.empoweredImpact && owner != null;
		for (UUID id : new ArrayList<>(field.captured)) {
			Entity entity = level.getEntity(id);
			if (!(entity instanceof LivingEntity target) || !target.isAlive()) continue;
			safeRelease(target);
			boolean frozen = EntityFreezeController.isFrozen(target);
			if (frozen) EntityFreezeController.neutralizeReleaseMotion(target);
			if (owner == null || !PowerProtection.mayForceMove(owner, target)
					|| AmethystDampening.isDampened(target)
					|| SpellFieldManager.blocksForcedMovement(level, target, owner.getUUID())
					|| frozen) continue;

			double horizontal = empowered ? field.collapseForce : 0.35;
			double downward = empowered ? 0.70 : 0.0;
			Vec3 impulse = GravityDisplacementRules.collapseImpulse(
					field.center, target.position(), horizontal, downward);
			if (!empowered) impulse = impulse.add(0.0, 0.16, 0.0);
			if (level.noBlockCollision(target, target.getBoundingBox().move(impulse))) {
				target.setDeltaMovement(impulse);
				target.hurtMarked = true;
			}
			if (empowered && PowerProtection.mayHarm(owner, target)
					&& !MagicShieldManager.global().active(
							target.getUUID(), level.getServer().getTickCount())) {
				target.hurtServer(level, PowerDamage.source(owner), field.impactDamage);
			}
		}
		field.captured.clear();
		TARGET_OWNERS.entrySet().removeIf(entry -> entry.getValue().equals(field.owner));
		GravityFx.collapse(level, field.center, field.radius, empowered,
				interrupted, field.ancientMastery);
	}

	private static void safeRelease(LivingEntity target) {
		target.fallDistance = 0.0F;
		target.addEffect(PowerStatusEffects.hidden(MobEffects.SLOW_FALLING,
				RELEASE_SLOW_FALL_TICKS, 0, true, true));
	}

	private static boolean shouldCue(GravityField field, UUID target, long now) {
		Long previous = field.resistanceCues.get(target);
		if (previous != null && now - previous < RESISTANCE_CUE_INTERVAL_TICKS) return false;
		field.resistanceCues.put(target, now);
		return true;
	}

	private static boolean claimTarget(ServerLevel level, LivingEntity target, GravityField candidate) {
		UUID targetId = target.getUUID();
		UUID currentOwner = TARGET_OWNERS.get(targetId);
		if (currentOwner == null || currentOwner.equals(candidate.owner)) {
			TARGET_OWNERS.put(targetId, candidate.owner);
			return true;
		}
		GravityField current = ACTIVE.get(currentOwner);
		if (current == null || !current.dimension.equals(candidate.dimension)) {
			TARGET_OWNERS.put(targetId, candidate.owner);
			return true;
		}
		double candidateDistance = target.position().distanceToSqr(candidate.center);
		double currentDistance = target.position().distanceToSqr(current.center);
		if (!GravityDisplacementRules.claimWinner(
				candidateDistance, currentDistance, CLAIM_HYSTERESIS_SQUARED)) return false;
		current.captured.remove(targetId);
		TARGET_OWNERS.put(targetId, candidate.owner);
		GravityFx.resonance(level, current.center, candidate.center,
				target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
		return true;
	}

	private static void releaseClaim(GravityField field, UUID target) {
		TARGET_OWNERS.remove(target, field.owner);
	}

	private static boolean ownsPower(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power != null && POWER_ID.equals(power.id())) return true;
		}
		return false;
	}

	/** Mutable state is deliberately private and owned by the server tick thread. */
	private static final class GravityField {
		private final UUID owner;
		private final ResourceKey<Level> dimension;
		private final CastSource castSource;
		private final Vec3 center;
		private final long openedAt;
		private final long expiresAt;
		private final double radius;
		private final int targetLimit;
		private final boolean empoweredImpact;
		private final boolean ancientMastery;
		private final float impactDamage;
		private final double collapseForce;
		private final Set<UUID> captured = new LinkedHashSet<>();
		private final Map<UUID, Long> resistanceCues = new HashMap<>();

		private GravityField(UUID owner, ResourceKey<Level> dimension, CastSource castSource, Vec3 center,
				long openedAt, long expiresAt, double radius, int targetLimit,
				boolean empoweredImpact, boolean ancientMastery, float impactDamage,
				double collapseForce) {
			this.owner = owner;
			this.dimension = dimension;
			this.castSource = castSource;
			this.center = center;
			this.openedAt = openedAt;
			this.expiresAt = expiresAt;
			this.radius = radius;
			this.targetLimit = targetLimit;
			this.empoweredImpact = empoweredImpact;
			this.ancientMastery = ancientMastery;
			this.impactDamage = impactDamage;
			this.collapseForce = collapseForce;
		}
	}
}

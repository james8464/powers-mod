package com.powers.power.abilities;

import com.powers.PowerStatusEffects;
import com.powers.PowersMod;
import com.powers.fx.SuperSpeedFx;
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
import com.powers.progression.ScaledMagicValues;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.PowerMessages;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

/** Owns finite Chronal Overdrive movement, rank consequences, and counterplay. */
public final class SuperSpeedAbility extends Ability {
	private static final Identifier POWER_ID = PowersMod.id("super_speed");
	private static final Identifier MODIFIER_ID = PowersMod.id("super_speed_overdrive");
	private static final int BASE_DURATION_TICKS = 160;
	private static final int MAX_ACTIVE_OVERDRIVES = 64;
	private static final int COLLISION_CUE_COOLDOWN = 8;
	private static final int AFTERIMAGE_INTERVAL = 20;
	private static final int MAX_PRESSURE_SCAN = 48;
	private static final int MAX_RESISTANCE_CUES = 8;
	private static final double MAX_TRAIL_DISTANCE = 12.0;
	private static final double REBOUND_BACKWARD = 0.85;
	private static final double REBOUND_UPWARD = 0.32;
	private static final double PRESSURE_RADIUS = 3.0;
	private static final double PRESSURE_STRENGTH = 1.0;
	private static final double PRESSURE_LIFT = 0.18;
	private static final double AFTERIMAGE_RADIUS = 8.0;
	private static final double PROJECTILE_RADIUS = 5.0;
	private static final double PROJECTILE_CURVE = 0.35;
	private static final double MAX_PROJECTILE_SPEED = 2.2;
	private static final Map<UUID, Overdrive> ACTIVE = new LinkedHashMap<>();

	public SuperSpeedAbility() {
		super(POWER_ID, Component.translatable(
				"ability.powers.super_speed"), 300, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		UUID owner = player.getUUID();
		if (!player.isAlive() || ACTIVE.containsKey(owner)
				|| ACTIVE.size() >= MAX_ACTIVE_OVERDRIVES) return false;
		AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (movement == null) return false;

		ServerLevel level = (ServerLevel) player.level();
		long now = level.getServer().getTickCount();
		ScaledMagicValues profile = scaling(player);
		Set<String> variants = profile.unlockedVariants();
		boolean secondStep = variants.contains("second_step");
		boolean empoweredImpact = variants.contains("empowered_impact");
		boolean afterimage = variants.contains("afterimage");
		boolean ancientMastery = variants.contains("ancient_mastery");
		int duration = scaledDuration(player, BASE_DURATION_TICKS);
		double potency = profile.potencyMultiplier();
		boolean inWater = player.isInWater();
		double modifier = SuperSpeedRules.speedModifier(potency, inWater);
		if (duration <= 0 || modifier <= 0.0) return false;

		Overdrive overdrive = new Overdrive(owner, level.dimension(),
				CastScalingContext.currentSource(), now, now + duration,
				potency, secondStep, empoweredImpact, afterimage, ancientMastery,
				player.position(), inWater,
				CombatTerrainImpact.tier(player, CastScalingContext.currentSource()));
		removeOwnedModifier(player);
		if (!applyOwnedModifier(player, overdrive, inWater)) return false;
		ACTIVE.put(owner, overdrive);
		player.fallDistance = 0.0F;
		SuperSpeedFx.open(level, player.position(), duration, secondStep,
				empoweredImpact, afterimage, ancientMastery);
		PowerMessages.send(player, "ability.powers.super_speed.cast", 4);
		return true;
	}

	/** Advances every active overdrive once from the authoritative server tick. */
	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		Iterator<Map.Entry<UUID, Overdrive>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			Overdrive overdrive = iterator.next().getValue();
			ServerPlayer owner = server.getPlayerList().getPlayer(overdrive.owner);
			if (now >= overdrive.expiresAt) {
				finish(server, overdrive, owner, false);
				iterator.remove();
				continue;
			}

			boolean sameDimension = owner != null
					&& owner.level().dimension().equals(overdrive.dimension);
			boolean dampened = owner != null && AmethystDampening.isDampened(owner);
			boolean frozen = MagicUseGate.timeLocked(owner);
			boolean ownsCast = owner != null && ServerCastLifecycle.mayContinue(
					owner, overdrive.castSource, ownsPower(owner));
			boolean continues = SuperSpeedRules.overdriveContinues(owner != null,
					sameDimension, owner != null && owner.isAlive() && !owner.isRemoved(),
					dampened, frozen, ownsCast, now,
					overdrive.expiresAt);
			if (!continues) {
				finish(server, overdrive, owner, true);
				iterator.remove();
				continue;
			}

			ServerLevel level = (ServerLevel) owner.level();
			boolean inWater = owner.isInWater();
			if (inWater != overdrive.inWater) {
				SuperSpeedFx.waterShift(level, owner.position(), inWater);
			}
			if (!applyOwnedModifier(owner, overdrive, inWater)) {
				finish(server, overdrive, owner, true);
				iterator.remove();
				continue;
			}
			int age = (int) Math.max(0L, now - overdrive.startedAt);
			tickWake(level, owner, overdrive, inWater, age);
			tickCollision(level, owner, overdrive, now);
			if (overdrive.afterimage && age > 0 && age % AFTERIMAGE_INTERVAL == 0) {
				slipHostileMemories(level, owner, overdrive);
			}
			if (overdrive.ancientMastery && (age & 1) == 0) {
				curveHostileProjectiles(level, owner, overdrive);
			}
			owner.fallDistance = 0.0F;
			overdrive.lastPosition = owner.position();
			overdrive.inWater = inWater;
			overdrive.wasHorizontalCollision = owner.horizontalCollision;
		}
	}

	/** Clears one owner's modifier and runtime state during respawn or disconnect. */
	public static void clear(MinecraftServer server, UUID owner) {
		Overdrive overdrive = ACTIVE.remove(owner);
		if (overdrive == null) return;
		ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(owner);
		finish(server, overdrive, player, true);
	}

	/** Releases all owned modifiers before server world references are discarded. */
	public static void clearAll(MinecraftServer server) {
		for (Overdrive overdrive : new ArrayList<>(ACTIVE.values())) {
			ServerPlayer player = server == null ? null
					: server.getPlayerList().getPlayer(overdrive.owner);
			finish(server, overdrive, player, true);
		}
		ACTIVE.clear();
	}

	/** Emits only finite movement samples and rejects teleports from the wake. */
	private static void tickWake(ServerLevel level, ServerPlayer owner,
			Overdrive overdrive, boolean inWater, int age) {
		Vec3 current = owner.position();
		double distanceSquared = overdrive.lastPosition.distanceToSqr(current);
		if (!SuperSpeedRules.trailAllowed(distanceSquared, MAX_TRAIL_DISTANCE)) return;
		int segments = SuperSpeedRules.trailSegments(Math.sqrt(distanceSquared));
		SuperSpeedFx.wake(level, overdrive.lastPosition, current, segments, inWater,
				age, overdrive.afterimage, overdrive.ancientMastery);
	}

	/** Resolves each new wall contact into its bounded rank consequences. */
	private static void tickCollision(ServerLevel level, ServerPlayer owner,
			Overdrive overdrive, long now) {
		boolean newCollision = owner.horizontalCollision && !overdrive.wasHorizontalCollision;
		if (!newCollision || now - overdrive.lastCollisionAt < COLLISION_CUE_COOLDOWN) return;
		overdrive.lastCollisionAt = now;
		Vec3 center = owner.position();
		CombatTerrainImpact.crater(level, owner, center, overdrive.terrainTier);
		SuperSpeedFx.collision(level, center.add(0.0, 0.8, 0.0));
		if (overdrive.secondStep && !overdrive.secondStepSpent) {
			applySecondStep(level, owner, overdrive, center);
		}
		if (overdrive.empoweredImpact && !overdrive.pressureSpent) {
			overdrive.pressureSpent = true;
			int moved = releasePressure(level, owner, center);
			SuperSpeedFx.pressure(level, center, moved);
			PowerMessages.send(owner, "ability.powers.super_speed.pressure", 3);
		}
	}

	/** Rewinds the runner once only when the complete moved body remains collision-free. */
	private static void applySecondStep(ServerLevel level, ServerPlayer owner,
			Overdrive overdrive, Vec3 from) {
		Vec3 rebound = SuperSpeedRules.rebound(
				owner.getLookAngle(), REBOUND_BACKWARD, REBOUND_UPWARD);
		if (rebound.equals(Vec3.ZERO)
				|| !level.noBlockCollision(owner, owner.getBoundingBox().move(rebound))) return;
		overdrive.secondStepSpent = true;
		owner.setDeltaMovement(rebound);
		owner.hurtMarked = true;
		owner.fallDistance = 0.0F;
		SuperSpeedFx.rebound(level, from, from.add(rebound));
		PowerMessages.send(owner, "ability.powers.super_speed.rebound", 3);
	}

	/** Applies the non-damaging pressure wave after exhaustive consent and ward checks. */
	private static int releasePressure(ServerLevel level, ServerPlayer owner, Vec3 center) {
		int limit = SuperSpeedRules.pressureTargetLimit(true);
		AABB area = AABB.ofSize(center, PRESSURE_RADIUS * 2.0,
				PRESSURE_RADIUS * 2.0, PRESSURE_RADIUS * 2.0);
		List<LivingEntity> candidates = BoundedEntityCandidates.living(level, area,
				MAX_PRESSURE_SCAN,
				entity -> entity != owner && entity.isAlive() && !entity.isSpectator()
						&& !entity.isPassenger()
						&& entity.position().distanceToSqr(center)
								<= PRESSURE_RADIUS * PRESSURE_RADIUS);
		candidates.sort(Comparator.comparingDouble((LivingEntity entity) ->
				entity.position().distanceToSqr(center)).thenComparing(
				entity -> entity.getUUID().toString()));

		int moved = 0;
		int resistanceCues = 0;
		for (int index = 0; index < Math.min(MAX_PRESSURE_SCAN, candidates.size()); index++) {
			if (moved >= limit) break;
			LivingEntity target = candidates.get(index);
			Vec3 impulse = SuperSpeedRules.pressureImpulse(
					center, target.position(), PRESSURE_STRENGTH, PRESSURE_LIFT);
			Vec3 velocity = target.getDeltaMovement().scale(0.35).add(impulse);
			boolean clearPath = !impulse.equals(Vec3.ZERO)
					&& level.noBlockCollision(target, target.getBoundingBox().move(velocity));
			boolean forcefield = target instanceof ServerPlayer player
					&& MagicShieldManager.global().active(
							player.getUUID(), level.getServer().getTickCount());
			SuperSpeedRules.PressureDecision decision = SuperSpeedRules.pressureDecision(
					PowerProtection.mayForceMove(owner, target),
					AmethystDampening.isDampened(target), BodyProxyManager.isProxy(target),
					forcefield, SpellFieldManager.blocksForcedMovement(
							level, target, owner.getUUID()),
					EntityFreezeController.isFrozen(target), clearPath);
			if (decision == SuperSpeedRules.PressureDecision.MOVE) {
				target.setDeltaMovement(velocity);
				target.hurtMarked = true;
				target.fallDistance = 0.0F;
				moved++;
			} else if (resistanceCues < MAX_RESISTANCE_CUES) {
				SuperSpeedFx.resistance(level, bodyCenter(target), decision);
				resistanceCues++;
			}
		}
		return moved;
	}

	/** Clears the runner from only nearby hostile target memories that currently see them. */
	private static void slipHostileMemories(ServerLevel level, ServerPlayer owner,
			Overdrive overdrive) {
		int limit = SuperSpeedRules.afterimageTargetLimit(overdrive.afterimage);
		if (limit <= 0) return;
		AABB area = AABB.ofSize(owner.position(), AFTERIMAGE_RADIUS * 2.0,
				AFTERIMAGE_RADIUS * 2.0, AFTERIMAGE_RADIUS * 2.0);
		List<Mob> mobs = BoundedEntityCandidates.ofClass(level, Mob.class, area, 64,
				mob -> mob.isAlive() && mob.getTarget() == owner && mob.hasLineOfSight(owner)
						&& !EntityFreezeController.isFrozen(mob)
						&& !PowerProtection.isSafeZone(level, mob.position())
						&& !SpellFieldManager.isSanctuaryProtected(level, mob));
		mobs.sort(Comparator.comparingDouble((Mob mob) -> mob.distanceToSqr(owner))
				.thenComparing(mob -> mob.getUUID().toString()));
		for (int index = 0; index < Math.min(limit, mobs.size()); index++) {
			Mob mob = mobs.get(index);
			mob.setTarget(null);
			SuperSpeedFx.memorySlip(level, owner.position(), mob.position(), index);
		}
	}

	/** Curves at most sixteen approaching non-owner projectiles once per cast. */
	private static void curveHostileProjectiles(ServerLevel level, ServerPlayer owner,
			Overdrive overdrive) {
		int limit = SuperSpeedRules.projectileLimit(overdrive.ancientMastery);
		if (limit <= 0 || overdrive.curvedProjectiles.size() >= limit) return;
		Vec3 center = owner.position().add(0.0, 0.8, 0.0);
		AABB area = AABB.ofSize(center, PROJECTILE_RADIUS * 2.0,
				PROJECTILE_RADIUS * 2.0, PROJECTILE_RADIUS * 2.0);
		List<Projectile> projectiles = BoundedEntityCandidates.ofClass(level, Projectile.class,
				area, 128,
				projectile -> projectile.isAlive()
						&& !overdrive.curvedProjectiles.contains(projectile.getUUID())
						&& (projectile.getOwner() == null
								|| (!projectile.getOwner().getUUID().equals(overdrive.owner)
										&& !owner.isAlliedTo(projectile.getOwner())))
						&& !PowerProtection.isSafeZone(level, projectile.position())
						&& projectile.position().distanceToSqr(center)
								<= PROJECTILE_RADIUS * PROJECTILE_RADIUS
						&& projectile.getDeltaMovement().dot(
								center.subtract(projectile.position())) > 0.0);
		projectiles.sort(Comparator.comparingDouble(
				projectile -> projectile.position().distanceToSqr(center)));
		for (Projectile projectile : projectiles) {
			if (overdrive.curvedProjectiles.size() >= limit) break;
			Vec3 curved = SuperSpeedRules.curveProjectile(projectile.position(),
					projectile.getDeltaMovement(), center,
					PROJECTILE_CURVE, MAX_PROJECTILE_SPEED);
			if (curved.equals(Vec3.ZERO)
					|| !level.noBlockCollision(projectile,
							projectile.getBoundingBox().move(curved))) continue;
			projectile.setDeltaMovement(curved);
			projectile.hurtMarked = true;
			overdrive.curvedProjectiles.add(projectile.getUUID());
			SuperSpeedFx.projectileCurve(level, projectile.position(), curved,
					overdrive.curvedProjectiles.size() - 1);
		}
	}

	/** Reconciles exactly one POWERS-owned movement modifier, including water grounding. */
	private static boolean applyOwnedModifier(ServerPlayer player,
			Overdrive overdrive, boolean inWater) {
		AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (movement == null) return false;
		double amount = SuperSpeedRules.speedModifier(overdrive.potencyMultiplier, inWater);
		if (amount <= 0.0) return false;
		if (movement.hasModifier(MODIFIER_ID)
				&& Math.abs(overdrive.appliedModifier - amount) <= 1.0E-9) return true;
		movement.removeModifier(MODIFIER_ID);
		movement.addTransientModifier(new AttributeModifier(MODIFIER_ID, amount,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		overdrive.appliedModifier = amount;
		return true;
	}

	/** Removes only Chronal Overdrive's own attribute contribution. */
	private static void removeOwnedModifier(ServerPlayer player) {
		AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (movement != null) movement.removeModifier(MODIFIER_ID);
	}

	/** Restores safe fall state and closes the visual seal exactly once. */
	private static void finish(MinecraftServer server, Overdrive overdrive,
			ServerPlayer player, boolean interrupted) {
		if (player != null) {
			removeOwnedModifier(player);
			player.fallDistance = 0.0F;
			if (player.isAlive()) {
				player.addEffect(PowerStatusEffects.hidden(MobEffects.SLOW_FALLING,
						60, 0, true, true));
			}
		}
		if (server == null) return;
		ServerLevel level = player != null
				&& player.level().dimension().equals(overdrive.dimension)
				? (ServerLevel) player.level() : server.getLevel(overdrive.dimension);
		if (level == null) return;
		Vec3 point = player != null && player.level() == level
				? player.position() : overdrive.lastPosition;
		boolean amethyst = player != null && AmethystDampening.isDampened(player);
		boolean frozen = MagicUseGate.timeLocked(player);
		SuperSpeedFx.finish(level, point, interrupted, amethyst, frozen,
				overdrive.ancientMastery);
	}

	/** Returns whether the owner still carries Super Speed in any configured slot. */
	private static boolean ownsPower(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power != null && power.id().equals(POWER_ID)) return true;
		}
		return false;
	}

	/** Returns the visual and collision center of one living body. */
	private static Vec3 bodyCenter(LivingEntity entity) {
		return entity.position().add(0.0, entity.getBbHeight() * 0.52, 0.0);
	}

	/** Mutable, server-thread-only state for one finite overdrive cast. */
	private static final class Overdrive {
		private final UUID owner;
		private final ResourceKey<Level> dimension;
		private final CastSource castSource;
		private final long startedAt;
		private final long expiresAt;
		private final double potencyMultiplier;
		private final boolean secondStep;
		private final boolean empoweredImpact;
		private final boolean afterimage;
		private final boolean ancientMastery;
		private final int terrainTier;
		private final Set<UUID> curvedProjectiles = new LinkedHashSet<>();
		private Vec3 lastPosition;
		private boolean inWater;
		private boolean wasHorizontalCollision;
		private boolean secondStepSpent;
		private boolean pressureSpent;
		private long lastCollisionAt = Long.MIN_VALUE / 2L;
		private double appliedModifier = Double.NaN;

		private Overdrive(UUID owner, ResourceKey<Level> dimension, CastSource castSource, long startedAt,
				long expiresAt, double potencyMultiplier, boolean secondStep,
				boolean empoweredImpact, boolean afterimage, boolean ancientMastery,
				Vec3 lastPosition, boolean inWater, int terrainTier) {
			this.owner = owner;
			this.dimension = dimension;
			this.castSource = castSource;
			this.startedAt = startedAt;
			this.expiresAt = expiresAt;
			this.potencyMultiplier = potencyMultiplier;
			this.secondStep = secondStep;
			this.empoweredImpact = empoweredImpact;
			this.afterimage = afterimage;
			this.ancientMastery = ancientMastery;
			this.terrainTier = terrainTier;
			this.lastPosition = lastPosition;
			this.inWater = inWater;
		}
	}
}

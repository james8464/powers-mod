package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.fx.FireballFx;
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
import com.powers.power.state.EntityFreezeController;
import com.powers.power.state.PowerEntityState;
import com.powers.progression.ScaledMagicValues;
import com.powers.spell.SpellFieldManager;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Owns chargeable Cinderheart fireballs from summon through finite impact. */
public final class FireballAbility extends Ability {
	private static final Identifier POWER_ID = PowersMod.id("fireball");
	private static final int BASE_HOVER_TICKS = 240;
	private static final int MAX_ACTIVE_HEARTS = 64;
	private static final int HOVER_PULSE_INTERVAL = 10;
	private static final double MAX_TRAIL_DISTANCE = 12.0;
	private static final Map<UUID, Cinderheart> BY_OWNER = new LinkedHashMap<>();
	private static final Map<UUID, Cinderheart> BY_PROJECTILE = new HashMap<>();

	public FireballAbility() {
		super(POWER_ID, Component.translatable("ability.powers.fireball"), 0, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		Cinderheart existing = BY_OWNER.get(player.getUUID());
		if (existing != null) {
			LargeFireball projectile = findProjectile(level.getServer(), existing);
			if (projectile == null || !existing.dimension.equals(level.dimension())
					|| level.getServer().getTickCount() >= existing.expiresAt) {
				if (projectile != null) {
					FireballFx.extinguish(level, projectile.position(), existing.tier,
							true, false, false);
				}
				removeState(existing, projectile, true);
			} else {
				return chargeExisting(player, projectile, existing);
			}
		}
		if (!player.isAlive() || BY_OWNER.size() >= MAX_ACTIVE_HEARTS) return false;

		Vec3 look = player.getLookAngle();
		if (!finiteDirection(look)) return false;
		Vec3 spawn = findSpawn(level, player, player.getEyePosition(), look.normalize());
		if (spawn == null) {
			FireballFx.blocked(level, player.getEyePosition().add(look.normalize().scale(1.2)));
			PowerMessages.send(player, "ability.powers.fireball.blocked", 3);
			return false;
		}

		LargeFireball projectile = new LargeFireball(level, player, Vec3.ZERO, 0);
		projectile.setPos(spawn);
		PowerEntityState.markPowerProjectile(projectile);
		if (!level.addFreshEntity(projectile)) return false;

		long now = level.getServer().getTickCount();
		ScaledMagicValues profile = scaling(player);
		Set<String> variants = profile.unlockedVariants();
		int hoverTicks = Math.min(360, scaledDuration(player, BASE_HOVER_TICKS));
		Cinderheart heart = new Cinderheart(player.getUUID(), projectile.getUUID(),
				CastScalingContext.currentSource(), CombatTerrainImpact.tier(
						player, CastScalingContext.currentSource(), "fireball"),
				level.dimension(), now, now + hoverTicks, profile.potencyMultiplier(),
				variants.contains("empowered_impact"), variants.contains("reflective_ward"),
				variants.contains("afterimage"), variants.contains("true_sight"),
				variants.contains("ancient_mastery"), spawn);
		BY_OWNER.put(heart.originalOwner, heart);
		BY_PROJECTILE.put(heart.projectile, heart);
		FireballFx.open(level, spawn, heart.tier, heart.empoweredImpact, heart.ancientMastery);
		PowerMessages.send(player, "ability.powers.fireball.cast", 4);
		return true;
	}

	@Override
	public void bindPhysicalPresence(ServerPlayer player, PlayerPowers.PlayerPowersData data,
			MagicPresenceId presenceId) {
		Cinderheart heart = BY_OWNER.get(player.getUUID());
		LargeFireball projectile = findProjectile(player.level().getServer(), heart);
		if (heart != null && projectile != null) {
			PhysicalMagicPresences.bindExistingEntity(presenceId, projectile,
					MagicPresenceHandle.Kind.PROJECTILE, heart.expiresAt);
		}
	}

	/** Advances all owned hearts without retaining Minecraft entity references. */
	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		Iterator<Map.Entry<UUID, Cinderheart>> iterator = BY_OWNER.entrySet().iterator();
		while (iterator.hasNext()) {
			Cinderheart heart = iterator.next().getValue();
			ServerPlayer originalOwner = server.getPlayerList().getPlayer(heart.originalOwner);
			ServerLevel level = server.getLevel(heart.dimension);
			LargeFireball projectile = findProjectile(server, heart);
			boolean sameDimension = originalOwner != null
					&& originalOwner.level().dimension().equals(heart.dimension);
			boolean dampened = originalOwner != null && AmethystDampening.isDampened(originalOwner);
			boolean frozen = MagicUseGate.timeLocked(originalOwner);
			boolean continues = FireballRules.continues(originalOwner != null, sameDimension,
					originalOwner != null && originalOwner.isAlive() && !originalOwner.isRemoved(),
					dampened, frozen, originalOwner != null && ServerCastLifecycle.mayContinue(
							originalOwner, heart.castSource, ownsFireSource(originalOwner)),
					now, heart.expiresAt);
			if (level == null || projectile == null || !continues) {
				if (level != null) {
					Vec3 point = projectile == null ? heart.lastPosition : projectile.position();
					FireballFx.extinguish(level, point, heart.tier, now >= heart.expiresAt,
							dampened, frozen);
				}
				BY_PROJECTILE.remove(heart.projectile);
				if (projectile != null) projectile.discard();
				iterator.remove();
				continue;
			}

			if (heart.launched && !controllerValid(server, heart)) {
				FireballFx.extinguish(level, projectile.position(), heart.tier,
						false, false, false);
				BY_PROJECTILE.remove(heart.projectile);
				projectile.discard();
				iterator.remove();
				continue;
			}
			if (!observeExternalController(projectile, heart)) {
				BY_PROJECTILE.remove(heart.projectile);
				projectile.discard();
				iterator.remove();
				continue;
			}

			int age = (int) Math.max(0L, now - heart.startedAt);
			if (!heart.launched) {
				projectile.setDeltaMovement(Vec3.ZERO);
				projectile.hurtMarked = true;
				if (age % HOVER_PULSE_INTERVAL == 0) {
					FireballFx.hover(level, projectile.position(), heart.tier, age,
							heart.afterimage, heart.trueSight, heart.ancientMastery);
				}
			} else if (interceptFlight(level, projectile, heart)) {
				BY_PROJECTILE.remove(heart.projectile);
				projectile.discard();
				iterator.remove();
				continue;
			} else {
				emitWake(level, projectile, heart, age);
			}
			heart.lastPosition = projectile.position();
		}
	}

	/** Clears one original caster's heart at respawn or disconnect. */
	public static void clear(MinecraftServer server, UUID owner) {
		Cinderheart heart = BY_OWNER.remove(owner);
		if (heart == null) return;
		BY_PROJECTILE.remove(heart.projectile);
		LargeFireball projectile = server == null ? null : findProjectile(server, heart);
		if (projectile != null) {
			if (projectile.level() instanceof ServerLevel level) {
				FireballFx.extinguish(level, projectile.position(), heart.tier,
						false, false, false);
			}
			projectile.discard();
		}
	}

	/** Clears every heart before server world references are discarded. */
	public static void clearAll(MinecraftServer server) {
		for (Cinderheart heart : new ArrayList<>(BY_OWNER.values())) {
			LargeFireball projectile = server == null ? null : findProjectile(server, heart);
			if (projectile != null) projectile.discard();
		}
		BY_OWNER.clear();
		BY_PROJECTILE.clear();
	}

	/** Authorizes the vanilla attack deflection hook and records finite controller transfer. */
	public static boolean allowDeflection(LargeFireball projectile,
			Entity deflectingEntity, boolean byAttack) {
		if (!(projectile.level() instanceof ServerLevel level)) return true;
		Cinderheart heart = BY_PROJECTILE.get(projectile.getUUID());
		if (heart == null) return false;
		if (!heart.launched) {
			if (!byAttack || !(deflectingEntity instanceof ServerPlayer controller)) {
				FireballFx.reflectionDenied(level, projectile.position(), 0);
				return false;
			}
			heart.launched = true;
			heart.controller = controller.getUUID();
			heart.expiresAt = FireballRules.launchExpiry(level.getServer().getTickCount());
			heart.lastPosition = projectile.position();
			FireballFx.launch(level, projectile.position(), controller.getLookAngle(),
					heart.tier, heart.ancientMastery);
			PowerMessages.send(controller, "ability.powers.fireball.launch", 3);
			return true;
		}

		int limit = FireballRules.reflectionLimit(heart.reflectiveWard, heart.ancientMastery);
		if (!FireballRules.reflectionAllowed(true, heart.reflections, limit)) {
			FireballFx.reflectionDenied(level, projectile.position(), heart.reflections);
			return false;
		}
		heart.reflections++;
		if (deflectingEntity instanceof ServerPlayer controller) {
			heart.controller = controller.getUUID();
		}
		Vec3 visualVelocity = deflectingEntity instanceof ServerPlayer controller
				? controller.getLookAngle() : projectile.getDeltaMovement();
		FireballFx.reflected(level, projectile.position(), visualVelocity,
				heart.reflections, limit);
		return true;
	}

	/** Replaces vanilla owned-fireball explosions with the controlled impact contract. */
	public static void resolveImpact(LargeFireball projectile, HitResult hit) {
		if (!(projectile.level() instanceof ServerLevel level)) return;
		Cinderheart heart = BY_PROJECTILE.get(projectile.getUUID());
		if (heart == null) {
			projectile.discard();
			return;
		}
		FireballImpactResolver.resolve(level, projectile, heart, hit);
	}

	/** Adds one paid tier and refreshes only the bounded hover window. */
	private boolean chargeExisting(ServerPlayer player, LargeFireball projectile,
			Cinderheart heart) {
		ServerLevel level = (ServerLevel) player.level();
		if (heart.launched) {
			FireballFx.chargeDenied(level, projectile.position(), heart.tier, heart.launched);
			PowerMessages.send(player, "ability.powers.fireball.sealed", 3);
			return false;
		}
		ScaledMagicValues profile = scaling(player);
		Set<String> variants = profile.unlockedVariants();
		heart.empoweredImpact |= variants.contains("empowered_impact");
		heart.afterimage |= variants.contains("afterimage");
		heart.trueSight |= variants.contains("true_sight");
		heart.ancientMastery |= variants.contains("ancient_mastery");
		if (!FireballRules.canCharge(heart.tier, heart.ancientMastery)) {
			FireballFx.chargeDenied(level, projectile.position(), heart.tier, false);
			PowerMessages.send(player, "ability.powers.fireball.sealed", 3);
			return false;
		}
		heart.tier = FireballRules.nextTier(heart.tier, heart.ancientMastery);
		heart.potencyMultiplier = Math.max(heart.potencyMultiplier, profile.potencyMultiplier());
		heart.expiresAt = FireballRules.extendedHoverExpiry(
				heart.startedAt, heart.expiresAt, level.getServer().getTickCount());
		FireballFx.charge(level, projectile.position(), heart.tier,
				heart.empoweredImpact, heart.ancientMastery);
		PowerMessages.send(player, "ability.powers.fireball.charge", 4, heart.tier);
		return true;
	}

	/** Intercepts dynamic wards, water, and frost between measured flight samples. */
	private static boolean interceptFlight(ServerLevel level, LargeFireball projectile,
			Cinderheart heart) {
		Vec3 current = projectile.position();
		SpellFieldManager.RayWardHit ward = SpellFieldManager.firstHarmfulRayIntercept(
				level, heart.controller, heart.lastPosition, current).orElse(null);
		if (ward != null) {
			FireballRules.ImpactDecision decision = switch (ward.counterplay()) {
				case SANCTUARY -> FireballRules.ImpactDecision.SANCTUARY;
				case KINETIC_WARD -> FireballRules.ImpactDecision.KINETIC_WARD;
				default -> null;
			};
			if (decision != null) {
				FireballFx.terminal(level, ward.point(), decision, heart.tier);
				return true;
			}
		}
		BlockState block = level.getBlockState(projectile.blockPosition());
		boolean water = projectile.isInWater() || block.getFluidState().is(FluidTags.WATER);
		boolean frost = block.is(BlockTags.ICE) || block.is(BlockTags.SNOW);
		if (!water && !frost) return false;
		FireballImpactResolver.resolveSteam(level, projectile, heart, current, frost);
		return true;
	}

	/** Emits only finite wake samples and ignores teleport-sized discontinuities. */
	private static void emitWake(ServerLevel level, LargeFireball projectile,
			Cinderheart heart, int age) {
		double distanceSquared = heart.lastPosition.distanceToSqr(projectile.position());
		if (!FireballRules.trailAllowed(distanceSquared, MAX_TRAIL_DISTANCE)) return;
		int segments = FireballRules.trailSegments(Math.sqrt(distanceSquared));
		FireballFx.wake(level, heart.lastPosition, projectile.position(), segments,
				heart.tier, age, heart.afterimage, heart.trueSight);
	}

	/** Observes ownership changes made by other server powers and enforces this heart's cap. */
	static boolean observeExternalController(LargeFireball projectile,
			Cinderheart heart) {
		if (!(projectile.getOwner() instanceof ServerPlayer controller)
				|| controller.getUUID().equals(heart.controller)) return true;
		int limit = FireballRules.reflectionLimit(heart.reflectiveWard, heart.ancientMastery);
		if (!FireballRules.reflectionAllowed(heart.launched, heart.reflections, limit)) {
			if (projectile.level() instanceof ServerLevel level) {
				FireballFx.reflectionDenied(level, projectile.position(), heart.reflections);
			}
			return false;
		}
		if (!heart.launched) {
			heart.launched = true;
			heart.expiresAt = FireballRules.launchExpiry(
					projectile.level().getServer().getTickCount());
		} else {
			heart.reflections++;
		}
		heart.controller = controller.getUUID();
		if (projectile.level() instanceof ServerLevel level) {
			FireballFx.reflected(level, projectile.position(), projectile.getDeltaMovement(),
					heart.reflections, limit);
		}
		return true;
	}

	/** Returns a clear one-block spawn volume between 1.5 and five blocks forward. */
	private static Vec3 findSpawn(ServerLevel level, ServerPlayer player, Vec3 eye, Vec3 look) {
		for (double distance = 1.5; distance <= 5.0; distance += 0.5) {
			Vec3 candidate = eye.add(look.scale(distance));
			AABB volume = AABB.ofSize(candidate, 1.0, 1.0, 1.0);
			if (level.getWorldBorder().isWithinBounds(volume)
					&& level.noBlockCollision(player, volume)) return candidate;
		}
		return null;
	}

	/** Detects wards and fluids before an impact helper sees protected game state. */
	private static boolean controllerValid(MinecraftServer server, Cinderheart heart) {
		ServerPlayer controller = server.getPlayerList().getPlayer(heart.controller);
		return controller != null && controller.isAlive() && !controller.isRemoved()
				&& controller.level().dimension().equals(heart.dimension);
	}

	/** Returns the live owned fireball without storing a strong entity reference. */
	private static LargeFireball findProjectile(MinecraftServer server, Cinderheart heart) {
		if (server == null || heart == null) return null;
		ServerLevel level = server.getLevel(heart.dimension);
		if (level == null) return null;
		Entity entity = level.getEntity(heart.projectile);
		return entity instanceof LargeFireball fireball && fireball.isAlive() ? fireball : null;
	}

	/** Removes both indexes and optionally discards the entity. */
	private static void removeState(Cinderheart heart, LargeFireball projectile,
			boolean discard) {
		BY_OWNER.remove(heart.originalOwner, heart);
		BY_PROJECTILE.remove(heart.projectile, heart);
		if (discard && projectile != null) projectile.discard();
	}

	/** Ends one controlled impact and records the vanilla projectile-land game event. */
	static void finishImpact(ServerLevel level, LargeFireball projectile,
			Cinderheart heart, Vec3 point) {
		level.gameEvent(GameEvent.PROJECTILE_LAND, point,
				GameEvent.Context.of(projectile, null));
		PhysicalMagicPresences.fixEntity(projectile, level, point,
				MagicPresenceHandle.Kind.IMPACT, level.getServer().getTickCount() + 20L);
		removeState(heart, projectile, true);
	}

	/** Returns whether the player's innate Fireball still owns this projectile. */
	private static boolean ownsFireSource(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power != null && power.id().equals(POWER_ID)) return true;
		}
		return false;
	}

	/** Returns whether an aim vector can safely be normalized. */
	private static boolean finiteDirection(Vec3 direction) {
		return direction != null && Double.isFinite(direction.x)
				&& Double.isFinite(direction.y) && Double.isFinite(direction.z)
				&& direction.lengthSqr() > 1.0E-12;
	}

}

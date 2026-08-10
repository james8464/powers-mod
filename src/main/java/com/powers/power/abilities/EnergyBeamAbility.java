package com.powers.power.abilities;

import com.powers.PowerStatusEffects;
import com.powers.PowersMod;
import com.powers.fx.EnergyBeamFx;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.ServerCastLifecycle;
import com.powers.mind.BodyProxyManager;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.Power;
import com.powers.power.PowerDamage;
import com.powers.power.state.EntityFreezeController;
import com.powers.progression.ScaledMagicValues;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.PowerMessages;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Channels four live-aim Sunfire beats with material, ward, and rank counterplay. */
public final class EnergyBeamAbility extends Ability {
	private static final Identifier POWER_ID = PowersMod.id("energy_beam");
	private static final double BASE_RANGE = 48.0;
	private static final float BASE_DAMAGE = 4.0F;
	private static final int BASE_BURN_TICKS = 60;
	private static final double STEAM_RADIUS = 3.0;
	private static final double FLARE_RADIUS = 3.5;
	private static final double SPLIT_RADIUS = 5.0;
	private static final double STEAM_FORCE = 0.8;
	private static final int MAX_ACTIVE_CHANNELS = 64;
	private static final Map<UUID, Channel> ACTIVE = new LinkedHashMap<>();

	public EnergyBeamAbility() {
		super(POWER_ID, net.minecraft.network.chat.Component.translatable(
				"ability.powers.energy_beam"), 80, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		UUID owner = player.getUUID();
		if (ACTIVE.containsKey(owner) || ACTIVE.size() >= MAX_ACTIVE_CHANNELS) return false;
		ScaledMagicValues scaled = scaling(player);
		Set<String> variants = scaled.unlockedVariants();
		long now = player.level().getServer().getTickCount();
		Channel channel = new Channel(player.level().dimension(), CastScalingContext.currentSource(), now,
				now + EnergyBeamRules.TOTAL_TICKS,
				Math.min(96.0, BASE_RANGE * scaled.rangeMultiplier()),
				(float) (BASE_DAMAGE * scaled.potencyMultiplier()),
				Math.max(20, (int) Math.round(BASE_BURN_TICKS * scaled.durationMultiplier())),
				variants.contains("empowered_impact"), variants.contains("ancient_mastery"),
				CombatTerrainImpact.tier(player, CastScalingContext.currentSource()),
				player.getEyePosition());
		ACTIVE.put(owner, channel);
		EnergyBeamFx.focus((ServerLevel) player.level(), channel.lastVisualPoint,
				EnergyBeamRules.FOCUS_TICKS, channel.ancientMastery);
		PowerMessages.send(player, "ability.powers.energy_beam.cast", 4);
		return true;
	}

	/** Advances all channels on the common authoritative end-server-tick callback. */
	public static void tickAll(MinecraftServer server) {
		long now = server.getTickCount();
		Iterator<Map.Entry<UUID, Channel>> iterator = ACTIVE.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Channel> entry = iterator.next();
			Channel channel = entry.getValue();
			ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
			ServerLevel originalLevel = server.getLevel(channel.dimension);
			if (now >= channel.expiresAt) {
				if (originalLevel != null) {
					EnergyBeamFx.complete(originalLevel, channel.lastVisualPoint,
							channel.ancientMastery);
				}
				iterator.remove();
				continue;
			}

			boolean sameDimension = owner != null
					&& owner.level().dimension().equals(channel.dimension);
			boolean dampened = owner != null && AmethystDampening.isDampened(owner);
			boolean frozen = owner != null && EntityFreezeController.isFrozen(owner);
			boolean ownsPower = owner != null && ServerCastLifecycle.mayContinue(
					owner, channel.castSource, ownsPower(owner));
			if (!EnergyBeamRules.channelContinues(owner != null, sameDimension,
					owner != null && owner.isAlive(), dampened, frozen, ownsPower,
					now, channel.expiresAt)) {
				if (originalLevel != null) {
					Vec3 point = sameDimension && owner != null
							? owner.getEyePosition() : channel.lastVisualPoint;
					EnergyBeamFx.interrupted(originalLevel, point, dampened);
				}
				iterator.remove();
				continue;
			}

			ServerLevel level = (ServerLevel) owner.level();
			channel.lastVisualPoint = owner.getEyePosition();
			long age = Math.max(0L, now - channel.startedAt);
			if ((age & 1L) == 0L) {
				owner.addEffect(PowerStatusEffects.hidden(MobEffects.SLOWNESS,
						4, 0, true, false));
			}
			if (EnergyBeamRules.phase(channel.startedAt, now) == EnergyBeamRules.Phase.FOCUS) {
				if ((age & 1L) == 0L) {
					EnergyBeamFx.focus(level, owner.getEyePosition(),
							EnergyBeamRules.focusRemaining(channel.startedAt, now),
							channel.ancientMastery);
				}
				continue;
			}

			boolean damageBeat = EnergyBeamRules.damageBeat(channel.startedAt, now);
			if (!damageBeat && (age & 1L) != 0L) continue;
			EnergyBeamRayResolver.RayResolution ray = EnergyBeamRayResolver.resolve(
					level, owner, channel.range);
			EnergyBeamFx.ray(level, owner.getEyePosition(), ray.endpoint(),
					damageBeat, channel.ancientMastery);
			if (damageBeat) resolveDamageBeat(level, owner, channel, ray);
		}
	}

	/** Drops one owner's runtime-only channel at respawn or disconnect. */
	public static void clear(UUID owner) {
		ACTIVE.remove(owner);
	}

	/** Drops every channel after the server has finished returning player bodies. */
	public static void clearAll() {
		ACTIVE.clear();
	}

	private static void resolveDamageBeat(ServerLevel level, ServerPlayer caster,
			Channel channel, EnergyBeamRayResolver.RayResolution ray) {
		if (ray.counterplay() == EnergyBeamRules.Counterplay.WATER) {
			resetStreak(channel);
			steamPulse(level, caster, channel, ray.endpoint());
			return;
		}
		if (ray.counterplay() == EnergyBeamRules.Counterplay.SURFACE) {
			resetStreak(channel);
			CombatTerrainImpact.rayScar(level, caster, caster.getEyePosition(),
					ray.endpoint(), channel.terrainTier, 0xFFD166);
			EnergyBeamFx.impact(level, ray.endpoint(), 1);
			return;
		}
		if (ray.counterplay() != null) {
			resetStreak(channel);
			EnergyBeamFx.countered(level, ray.endpoint(), ray.counterplay());
			return;
		}
		if (ray.target() == null) {
			resetStreak(channel);
			return;
		}

		EnergyBeamRules.Counterplay counterplay = EnergyBeamRayResolver.targetCounter(
				level, caster, ray.target(), caster.getEyePosition());
		float attemptedDamage = (float) EnergyBeamRules.scorchDamage(
				channel.baseDamage, channel.lastTarget != null
						&& channel.lastTarget.equals(ray.target().getUUID()) ? channel.streak + 1 : 1);
		if (counterplay != null) {
			resetStreak(channel);
			if (counterplay == EnergyBeamRules.Counterplay.FORCEFIELD) {
				ray.target().hurtServer(level, PowerDamage.source(caster), attemptedDamage);
			}
			EnergyBeamFx.countered(level, ray.endpoint(), counterplay);
			return;
		}

		boolean sameTarget = ray.target().getUUID().equals(channel.lastTarget);
		int streak = EnergyBeamRules.nextStreak(sameTarget, channel.streak);
		float damage = (float) EnergyBeamRules.scorchDamage(channel.baseDamage, streak);
		if (!ray.target().hurtServer(level, PowerDamage.source(caster), damage)) {
			resetStreak(channel);
			EnergyBeamFx.countered(level, ray.endpoint(), EnergyBeamRules.Counterplay.RESISTED);
			return;
		}
		channel.lastTarget = ray.target().getUUID();
		channel.streak = streak;
		ray.target().setRemainingFireTicks(Math.max(ray.target().getRemainingFireTicks(),
				EnergyBeamRules.burnTicks(channel.baseBurnTicks, streak)));
		EnergyBeamFx.impact(level, ray.endpoint(), streak);

		if (EnergyBeamRules.flareReady(channel.empoweredImpact, streak,
				channel.flared, channel.flared ? 1 : 0)) {
			channel.flared = true;
			solarFlare(level, caster, channel, ray.target(), ray.endpoint(), damage);
		}
		ancientSplits(level, caster, channel, ray.target(), ray.endpoint(), damage);
	}

	private static void steamPulse(ServerLevel level, ServerPlayer caster,
			Channel channel, Vec3 center) {
		EnergyBeamFx.steam(level, center);
		float damage = (float) EnergyBeamRules.steamDamage(channel.baseDamage);
		for (LivingEntity target : EnergyBeamRayResolver.nearbyTargets(
				level, caster, center, STEAM_RADIUS,
				EnergyBeamRules.auxiliaryTargetLimit(), null)) {
			EnergyBeamRules.Counterplay counterplay = EnergyBeamRayResolver.targetCounter(
					level, caster, target, center);
			if (counterplay != null) {
				if (counterplay == EnergyBeamRules.Counterplay.FORCEFIELD) {
					target.hurtServer(level, PowerDamage.source(caster), damage);
				}
				EnergyBeamFx.countered(level, EnergyBeamRayResolver.bodyCenter(target), counterplay);
				continue;
			}
			if (!target.hurtServer(level, PowerDamage.source(caster), damage)) {
				EnergyBeamFx.countered(level, EnergyBeamRayResolver.bodyCenter(target),
						EnergyBeamRules.Counterplay.RESISTED);
				continue;
			}
			if (!BodyProxyManager.isProxy(target) && !EntityFreezeController.isFrozen(target)
					&& PowerProtection.mayForceMove(caster, target)
					&& !SpellFieldManager.blocksForcedMovement(level, target, caster.getUUID())) {
				Vec3 impulse = EnergyBeamRules.steamImpulse(center, target.position(), STEAM_FORCE);
				if (level.noBlockCollision(target, target.getBoundingBox().move(impulse))) {
					target.setDeltaMovement(target.getDeltaMovement().add(impulse));
					target.hurtMarked = true;
				}
			}
		}
	}

	private static void solarFlare(ServerLevel level, ServerPlayer caster, Channel channel,
			LivingEntity primary, Vec3 center, float primaryDamage) {
		EnergyBeamFx.flare(level, center);
		float flareDamage = Math.max(channel.baseDamage * 0.5F, primaryDamage * 0.45F);
		for (LivingEntity target : EnergyBeamRayResolver.nearbyTargets(
				level, caster, center, FLARE_RADIUS,
				EnergyBeamRules.auxiliaryTargetLimit(), primary)) {
			EnergyBeamRules.Counterplay counterplay = EnergyBeamRayResolver.targetCounter(
					level, caster, target, center);
			if (counterplay != null) {
				if (counterplay == EnergyBeamRules.Counterplay.FORCEFIELD) {
					target.hurtServer(level, PowerDamage.source(caster), flareDamage);
				}
				EnergyBeamFx.countered(level, EnergyBeamRayResolver.bodyCenter(target), counterplay);
				continue;
			}
			if (!target.hurtServer(level, PowerDamage.source(caster), flareDamage)) {
				EnergyBeamFx.countered(level, EnergyBeamRayResolver.bodyCenter(target),
						EnergyBeamRules.Counterplay.RESISTED);
			}
		}
	}

	private static void ancientSplits(ServerLevel level, ServerPlayer caster, Channel channel,
			LivingEntity primary, Vec3 impact, float primaryDamage) {
		int limit = EnergyBeamRules.splitLimit(channel.ancientMastery);
		if (limit <= 0) return;
		float damage = (float) EnergyBeamRules.splitDamage(primaryDamage);
		int index = 0;
		for (LivingEntity target : EnergyBeamRayResolver.nearbyTargets(
				level, caster, impact, SPLIT_RADIUS,
				limit * 3, primary)) {
			if (index >= limit || !primary.hasLineOfSight(target)) continue;
			EnergyBeamRules.Counterplay counterplay = EnergyBeamRayResolver.targetCounter(
					level, caster, target, impact);
			if (counterplay != null) {
				if (counterplay == EnergyBeamRules.Counterplay.FORCEFIELD) {
					target.hurtServer(level, PowerDamage.source(caster), damage);
				}
				EnergyBeamFx.countered(level, EnergyBeamRayResolver.bodyCenter(target), counterplay);
				continue;
			}
			if (!target.hurtServer(level, PowerDamage.source(caster), damage)) {
				EnergyBeamFx.countered(level, EnergyBeamRayResolver.bodyCenter(target),
						EnergyBeamRules.Counterplay.RESISTED);
				continue;
			}
			Vec3 targetCenter = EnergyBeamRayResolver.bodyCenter(target);
			EnergyBeamFx.split(level, impact, targetCenter, index);
			index++;
		}
	}

	private static boolean ownsPower(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power != null && power.id().equals(POWER_ID)) return true;
		}
		return false;
	}

	private static void resetStreak(Channel channel) {
		channel.lastTarget = null;
		channel.streak = 0;
	}

	/** Mutable channel state is private and accessed only from the server tick thread. */
	private static final class Channel {
		private final ResourceKey<Level> dimension;
		private final CastSource castSource;
		private final long startedAt;
		private final long expiresAt;
		private final double range;
		private final float baseDamage;
		private final int baseBurnTicks;
		private final boolean empoweredImpact;
		private final boolean ancientMastery;
		private final int terrainTier;
		private Vec3 lastVisualPoint;
		private UUID lastTarget;
		private int streak;
		private boolean flared;

		private Channel(ResourceKey<Level> dimension, CastSource castSource,
				long startedAt, long expiresAt,
				double range, float baseDamage, int baseBurnTicks, boolean empoweredImpact,
				boolean ancientMastery, int terrainTier, Vec3 lastVisualPoint) {
			this.dimension = dimension;
			this.castSource = castSource;
			this.startedAt = startedAt;
			this.expiresAt = expiresAt;
			this.range = range;
			this.baseDamage = baseDamage;
			this.baseBurnTicks = baseBurnTicks;
			this.empoweredImpact = empoweredImpact;
			this.ancientMastery = ancientMastery;
			this.terrainTier = Math.clamp(terrainTier, 0, 10);
			this.lastVisualPoint = lastVisualPoint;
		}
	}
}

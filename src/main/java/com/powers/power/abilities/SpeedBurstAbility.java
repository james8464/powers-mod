package com.powers.power.abilities;

import com.powers.PowerStatusEffects;
import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.ServerCastLifecycle;
import com.powers.network.PowersPackets;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.ActivationCooldowns;
import com.powers.power.AmethystDampening;
import com.powers.power.MagicUseGate;
import com.powers.power.Power;
import com.powers.power.PowerDamage;
import com.powers.progression.PowerScalingService;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.mind.BodyProxyManager;
import com.powers.power.state.EntityFreezeController;
import com.powers.power.state.MagicShieldManager;
import com.powers.util.PowerMessages;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Server-synchronized kinetic dash with a bounded wake, impact, and ranked follow-up. */
public final class SpeedBurstAbility extends Ability {
	private static final net.minecraft.resources.Identifier POWER_ID = PowersMod.id("speed_burst");
	private static final double BASE_STRENGTH = 2.2;
	private static final double SECOND_STEP_MULTIPLIER = 1.15;
	private static final double MINIMUM_VERTICAL = -0.35;
	private static final double MAXIMUM_VERTICAL = 0.80;
	private static final int COLLISION_SAMPLES = 12;
	private static final int TRACE_TICKS = 8;
	private static final int SECOND_STEP_DELAY = 2;
	private static final int SECOND_STEP_WINDOW = 50;
	private static final double IMPACT_RADIUS = 3.0;
	private static final int MAX_IMPACT_TARGETS = 12;
	private static final float BASE_IMPACT_DAMAGE = 4.0F;
	private static final float BASE_IMPACT_FORCE = 1.35F;

	private static final Map<UUID, DashTrace> DASHES = new HashMap<>();
	private static final Map<UUID, SecondStepWindow> SECOND_STEPS = new HashMap<>();

	public SpeedBurstAbility() {
		super(POWER_ID,
				Component.translatable("ability.powers.speed_burst"), 140, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		long now = level.getServer().getTickCount();
		CastSource castSource = CastScalingContext.currentSource();
		boolean mastered = hasSecondStep(player);
		boolean followUp = availableSecondStep(player, now, mastered);
		double strength = BASE_STRENGTH * Math.min(1.35, scaling(player).rangeMultiplier())
				* (followUp ? SECOND_STEP_MULTIPLIER : 1.0);
		Vec3 impulse = SpeedBurstRules.dashVector(
				player.getLookAngle(), strength, MINIMUM_VERTICAL, MAXIMUM_VERTICAL);
		if (impulse.lengthSqr() == 0.0) return false;
		if (followUp) SECOND_STEPS.remove(player.getUUID());

		DashTrace previous = DASHES.remove(player.getUUID());
		if (previous != null && previous.dimension().equals(level.dimension())) {
			emitImpact(level, player, player.position(), previous);
		}

		double safeFraction = collisionFraction(level, player, impulse);
		Vec3 movement = impulse.scale(safeFraction);
		player.setDeltaMovement(movement);
		player.hurtMarked = true;
		player.fallDistance = 0.0F;
		player.addEffect(PowerStatusEffects.hidden(MobEffects.SLOW_FALLING,
				scaledDuration(player, 120), 0, false, true));

		DashTrace trace = new DashTrace(level.dimension(), castSource, player.position(), TRACE_TICKS,
				safeFraction < 1.0, followUp, scaledPotency(player, BASE_IMPACT_DAMAGE),
				scaledPotency(player, BASE_IMPACT_FORCE));
		DASHES.put(player.getUUID(), trace);
		PowerFx.speedBurstRelease(level, player.position().add(0.0, 0.45, 0.0), movement, followUp);

		if (!followUp && mastered) {
			SECOND_STEPS.put(player.getUUID(), new SecondStepWindow(
					level.dimension(), castSource, now + SECOND_STEP_DELAY, now + SECOND_STEP_WINDOW));
			PowerFx.secondStepReady(level, player.position().add(0.0, 0.45, 0.0));
			PowerMessages.send(player, "ability.powers.speed_burst.second_step", 3);
		} else if (!mastered) {
			SECOND_STEPS.remove(player.getUUID());
		}
		return true;
	}

	@Override
	public boolean mayReactivateDuringCooldown(ServerPlayer player,
			PlayerPowers.PlayerPowersData data, int remainingTicks) {
		if (remainingTicks <= 0) return false;
		SecondStepWindow window = validWindow(player);
		return window != null && SpeedBurstRules.secondStepAvailable(
				window.opensAt(), window.expiresAt(), player.level().getServer().getTickCount(), true);
	}

	@Override
	public int reactivationTicks(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		SecondStepWindow window = validWindow(player);
		if (window == null || ActivationCooldowns.remainingTicks(player, this) <= 0) return 0;
		long now = player.level().getServer().getTickCount();
		if (!SpeedBurstRules.secondStepAvailable(
				window.opensAt(), window.expiresAt(), now, true)) return 0;
		return SpeedBurstRules.secondStepRemaining(
				window.expiresAt(), now, true);
	}

	/** Advances all short kinetic wakes and evicts expired ranked follow-ups. */
	public static void tickAll(MinecraftServer server) {
		tickWindows(server);
		Iterator<Map.Entry<UUID, DashTrace>> traces = DASHES.entrySet().iterator();
		while (traces.hasNext()) {
			Map.Entry<UUID, DashTrace> entry = traces.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			DashTrace trace = entry.getValue();
			if (player == null || !player.isAlive() || !trace.dimension().equals(player.level().dimension())
					|| !MagicUseGate.ongoingAllowed(player)
					|| !ServerCastLifecycle.mayContinue(player, trace.castSource(), ownsPower(player))) {
				traces.remove();
				continue;
			}
			ServerLevel level = (ServerLevel) player.level();
			Vec3 current = player.position();
			int remaining = trace.remainingTicks() - 1;
			PowerFx.speedBurstWake(level, trace.lastPosition(), current, trace.followUp(),
					TRACE_TICKS - remaining);
			if (SpeedBurstRules.traceFinished(
					trace.predictedObstruction(), player.horizontalCollision, remaining)) {
				emitImpact(level, player, current, trace);
				traces.remove();
			} else {
				entry.setValue(trace.advance(current, remaining));
			}
		}
	}

	/** Removes one owner's ephemeral dash and follow-up state without touching cooldowns. */
	public static void clear(UUID owner) {
		DASHES.remove(owner);
		SECOND_STEPS.remove(owner);
	}

	/** Clears every runtime-only dash state during server shutdown. */
	public static void clearAll() {
		DASHES.clear();
		SECOND_STEPS.clear();
	}

	/** Evicts invalid windows and publishes their exact open/closed HUD transitions. */
	private static void tickWindows(MinecraftServer server) {
		long now = server.getTickCount();
		Iterator<Map.Entry<UUID, SecondStepWindow>> windows = SECOND_STEPS.entrySet().iterator();
		while (windows.hasNext()) {
			Map.Entry<UUID, SecondStepWindow> entry = windows.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			SecondStepWindow window = entry.getValue();
			boolean invalid = player == null || !player.isAlive()
					|| !window.dimension().equals(player.level().dimension())
					|| !PowerScalingService.hasVariant(player, "second_step")
					|| !MagicUseGate.ongoingAllowed(player)
					|| !ServerCastLifecycle.mayContinue(player, window.castSource(), ownsPower(player))
					|| now >= window.expiresAt();
			if (invalid) {
				windows.remove();
				if (player != null) PowersPackets.syncTo(player);
			} else if (now == window.opensAt()) {
				PowersPackets.syncTo(player);
			}
		}
	}

	/** Returns an owner window only while its mastery, dimension, life, and time remain valid. */
	private SecondStepWindow validWindow(ServerPlayer player) {
		SecondStepWindow window = SECOND_STEPS.get(player.getUUID());
		if (window == null) return null;
		long now = player.level().getServer().getTickCount();
		if (!player.isAlive() || !hasSecondStep(player)
				|| !window.dimension().equals(player.level().dimension())
				|| !MagicUseGate.ongoingAllowed(player)
				|| !ServerCastLifecycle.mayContinue(player, window.castSource(), ownsPower(player))
				|| now >= window.expiresAt()) {
			SECOND_STEPS.remove(player.getUUID());
			return null;
		}
		return window;
	}

	/** Classifies a follow-up without consuming it so failed dash geometry can still refund safely. */
	private boolean availableSecondStep(ServerPlayer player, long now, boolean mastered) {
		SecondStepWindow window = SECOND_STEPS.get(player.getUUID());
		boolean cooldownArmed = ActivationCooldowns.remainingTicks(player, this) > 0;
		if (window != null && window.dimension().equals(player.level().dimension()) && cooldownArmed
				&& MagicUseGate.ongoingAllowed(player)
				&& ServerCastLifecycle.mayContinue(player, window.castSource(), ownsPower(player))
				&& SpeedBurstRules.secondStepAvailable(
						window.opensAt(), window.expiresAt(), now, mastered)) {
			return true;
		}
		if (window != null && (!mastered || now >= window.expiresAt()
				|| !window.dimension().equals(player.level().dimension()) || !cooldownArmed)) {
			SECOND_STEPS.remove(player.getUUID());
		}
		return false;
	}

	private boolean hasSecondStep(ServerPlayer player) {
		return scaling(player).unlockedVariants().contains("second_step");
	}

	private static boolean ownsPower(ServerPlayer player) {
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		for (int slot = 0; slot < PlayerPowers.SLOT_COUNT; slot++) {
			Power power = data.getPower(slot);
			if (power != null && POWER_ID.equals(power.id())) return true;
		}
		return false;
	}

	/** Samples the moved body in order and never accepts clear space beyond the first obstruction. */
	private static double collisionFraction(ServerLevel level, ServerPlayer player, Vec3 impulse) {
		boolean[] clear = new boolean[COLLISION_SAMPLES];
		AABB body = player.getBoundingBox();
		for (int sample = 0; sample < COLLISION_SAMPLES; sample++) {
			double progress = (sample + 1.0) / COLLISION_SAMPLES;
			clear[sample] = level.noBlockCollision(player, body.move(impulse.scale(progress)));
			if (!clear[sample]) break;
		}
		return SpeedBurstRules.lastSafeFraction(clear);
	}

	/** Applies one terrain-safe, nearest-target-capped endpoint shockwave. */
	private static void emitImpact(ServerLevel level, ServerPlayer caster, Vec3 center, DashTrace trace) {
		PowerFx.speedBurstImpact(level, center.add(0.0, 0.35, 0.0), trace.followUp());
		AABB area = AABB.ofSize(center, IMPACT_RADIUS * 2.0,
				IMPACT_RADIUS * 2.0, IMPACT_RADIUS * 2.0);
		BoundedEntityCandidates.living(level, area, 64,
				entity -> validImpactTarget(caster, entity))
				.stream()
				.sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)))
				.limit(MAX_IMPACT_TARGETS)
				.forEach(target -> applyImpact(level, caster, target, center, trace));
	}

	/** Requires exposure and at least one permitted harm or movement outcome. */
	private static boolean validImpactTarget(ServerPlayer caster, LivingEntity target) {
		return target != caster && target.isAlive() && !AmethystDampening.isDampened(target)
				&& caster.hasLineOfSight(target)
				&& (mayDamage((ServerLevel) caster.level(), caster, target)
				|| mayMove((ServerLevel) caster.level(), caster, target));
	}

	/** Keeps damage and forced movement as independent protection-policy decisions. */
	private static void applyImpact(ServerLevel level, ServerPlayer caster, LivingEntity target,
			Vec3 center, DashTrace trace) {
		if (mayDamage(level, caster, target)) {
			target.hurtServer(level, PowerDamage.source(caster), trace.damage());
		}
		if (!mayMove(level, caster, target)) return;
		Vec3 impulse = SpeedBurstRules.impactImpulse(center, target.position(), trace.force());
		impulse = ControlResistance.adjustImpulse(impulse, ControlResistance.outcome(target));
		if (impulse.lengthSqr() == 0.0) return;
		target.push(impulse.x, impulse.y, impulse.z);
		target.hurtMarked = true;
	}

	private static boolean mayDamage(ServerLevel level, ServerPlayer caster, LivingEntity target) {
		return PowerProtection.mayHarm(caster, target)
				&& !SpellFieldManager.isSanctuaryProtected(level, target);
	}

	private static boolean mayMove(ServerLevel level, ServerPlayer caster, LivingEntity target) {
		return !BodyProxyManager.isProxy(target) && !EntityFreezeController.isFrozen(target)
				&& PowerProtection.mayForceMove(caster, target)
				&& !SpellFieldManager.blocksForcedMovement(level, target, caster.getUUID())
				&& !MagicShieldManager.global().active(
						target.getUUID(), level.getServer().getTickCount());
	}

	/** Immutable wake state snapshots potency while the prepared cast adjustment is active. */
	private record DashTrace(ResourceKey<Level> dimension, CastSource castSource,
			Vec3 lastPosition, int remainingTicks,
			boolean predictedObstruction, boolean followUp, float damage, float force) {
		DashTrace advance(Vec3 position, int ticks) {
			return new DashTrace(dimension, castSource, position, ticks, false, followUp, damage, force);
		}
	}

	/** Runtime-only cooldown bypass bounded to one owner and dimension. */
	private record SecondStepWindow(ResourceKey<Level> dimension, CastSource castSource,
			long opensAt, long expiresAt) {
	}
}

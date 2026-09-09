package com.powers.power.crystals;

import com.powers.PowerStatusEffects;
import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.magic.runtime.CastScalingContext;
import com.powers.magic.runtime.CastSource;
import com.powers.magic.runtime.ServerCastLifecycle;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.MagicUseGate;
import com.powers.power.scaling.SafeScaleTransitionService;
import com.powers.util.PowerMessages;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Size Shift: the Yellow Crystal's power. You bend the size of your own
 * body: shrink to a speck that slips past every blade, or tower over your
 * enemies and crush them. Each activation alternates to the other size.
 */
public class SizeShiftAbility extends Ability {
	private static final int DURATION_TICKS = 400;
	private static final int COOLDOWN_TICKS = 600;

	// Hard scale endpoints keep Yellow within Minecraft's approved practical range.
	private static final AttributeModifier SHRINK_MODIFIER = new AttributeModifier(
			PowersMod.id("size_shift_shrink"),
			CrystalSizeShiftRules.modifierFor(CrystalSizeShiftRules.SMALL_SCALE),
			AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	private static final AttributeModifier GROW_MODIFIER = new AttributeModifier(
			PowersMod.id("size_shift_grow"),
			CrystalSizeShiftRules.modifierFor(CrystalSizeShiftRules.GIANT_SCALE),
			AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	private static final AttributeModifier ANTI_KNOCKBACK = new AttributeModifier(
			PowersMod.id("size_shift_knockback"), 1.0, AttributeModifier.Operation.ADD_VALUE);

	// Selection and cleanup tokens are runtime-only and are cleared at lifecycle edges.
	private static final Map<UUID, Integer> LAST_SIZE = new HashMap<>();
	private static final Map<UUID, ActiveCast> ACTIVE_CASTS = new HashMap<>();
	private record ActiveCast(CastSource castSource, long expiresAt) { }

	public SizeShiftAbility() {
		super(PowersMod.id("size_shift"),
				Component.translatable("ability.powers.size_shift"),
				COOLDOWN_TICKS, false, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		if (!CrystalSizeShiftRules.mayActivate(player.isPassenger())) {
			PowerMessages.overlay(player, Component.translatable("ability.powers.size_shift.mounted"));
			return false;
		}
		int next = LAST_SIZE.getOrDefault(player.getUUID(), 2) == 1 ? 2 : 1;
		ServerLevel level = (ServerLevel) player.level();
		double targetScale = CrystalSizeShiftRules.nextScale(
				LAST_SIZE.getOrDefault(player.getUUID(), 2));
		var transition = SafeScaleTransitionService.preview(
				level, player, List.of(targetScale), true, false, 1.5);
		if (transition == null) {
			PowerMessages.overlay(player, Component.translatable("ability.powers.size_shift.blocked"));
			return false;
		}
		removeOwnedModifiers(player);
		var modifier = next == 1 ? SHRINK_MODIFIER : GROW_MODIFIER;
		if (!SafeScaleTransitionService.apply(level, player, modifier.id(), transition)) return false;
		LAST_SIZE.put(player.getUUID(), next);
		int duration = scaledDuration(player, DURATION_TICKS);
		AttributeInstance knockback = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (next == 1) {
			player.addEffect(PowerStatusEffects.hidden(MobEffects.SPEED,
					duration, 5, true, true));
			player.addEffect(PowerStatusEffects.hidden(MobEffects.JUMP_BOOST,
					duration, 6, true, true));
			player.addEffect(PowerStatusEffects.hidden(MobEffects.SLOW_FALLING,
					duration, 0, true, true));
		} else {
			if (knockback != null) {
				knockback.addOrUpdateTransientModifier(ANTI_KNOCKBACK);
			}
			player.addEffect(PowerStatusEffects.hidden(MobEffects.STRENGTH,
					duration, 9, true, true));
			player.addEffect(PowerStatusEffects.hidden(MobEffects.RESISTANCE,
					duration, 3, true, true));
		}
		emitShockwave(level, player, targetScale);
		PowerFx.rune(level, player.position(), next == 1 ? 1.15 : 2.15,
				next == 1 ? 0x00E5FF : 0xFFD600, 28, next == 1 ? 0.0 : Math.PI);
		PowerFx.sound(level, player.position(), SoundEvents.PLAYER_HURT_FREEZE, 1.0f,
				CrystalSizeShiftRules.comfortPitch(targetScale));

		ACTIVE_CASTS.put(player.getUUID(), new ActiveCast(CastScalingContext.currentSource(),
				level.getServer().getTickCount() + duration));
		return true;
	}

	/** Ends transformations at their deadline or as soon as routed artifact authority is lost. */
	public static void tickAll(net.minecraft.server.MinecraftServer server) {
		long now = server.getTickCount();
		for (var iterator = ACTIVE_CASTS.entrySet().iterator(); iterator.hasNext();) {
			var entry = iterator.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			ActiveCast active = entry.getValue();
			if (player != null && player.isPassenger()) {
				if (!normalizeIfSafe(player)) player.stopRiding();
				else iterator.remove();
				PowerMessages.overlay(player, Component.translatable("ability.powers.size_shift.mounted"));
				continue;
			}
			if (player != null && now < active.expiresAt()
					&& MagicUseGate.ongoingAllowed(player)
					&& ServerCastLifecycle.mayContinue(player, active.castSource(), false)) continue;
			if (player == null || player.isRemoved()) {
				iterator.remove();
				continue;
			}
			if (!normalizeIfSafe(player)) continue;
			iterator.remove();
			PowerFx.burst((ServerLevel) player.level(), player.position().add(0, 1, 0),
					ParticleTypes.POOF, 16, 0.8, 0.2);
		}
	}

	private static boolean normalizeIfSafe(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) return false;
		var transition = SafeScaleTransitionService.previewNormal(level, player);
		if (transition == null) return false;
		removeOwnedModifiers(player);
		player.setPose(transition.pose());
		player.refreshDimensions();
		return level.noCollision(player, player.getBoundingBox());
	}

	private static void emitShockwave(ServerLevel level, ServerPlayer player, double scale) {
		double radius = CrystalSizeShiftRules.shockwaveRadius(scale);
		int color = scale >= 1.0 ? 0xFFD600 : 0x00E5FF;
		PowerFx.ring(level, player.position().add(0, 0.15, 0), radius, color, 32, 0.0);
		int affected = 0;
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
				player.getBoundingBox().inflate(radius), entity -> entity != player && entity.isAlive())) {
			if (affected++ >= 16) break;
			Vec3 outward = target.position().subtract(player.position());
			double horizontal = Math.hypot(outward.x, outward.z);
			if (horizontal < 1.0E-6) continue;
			double strength = scale >= 1.0 ? 0.55 : 0.18;
			target.push(outward.x / horizontal * strength, 0.12, outward.z / horizontal * strength);
		}
	}

	private static void removeOwnedModifiers(ServerPlayer player) {
		AttributeInstance scale = player.getAttribute(Attributes.SCALE);
		if (scale != null) {
			scale.removeModifier(SHRINK_MODIFIER);
			scale.removeModifier(GROW_MODIFIER);
		}
		AttributeInstance knockback = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (knockback != null) knockback.removeModifier(ANTI_KNOCKBACK);
	}

	/** Drops one player's transient selection and transformation state. */
	public static void clear(UUID player) {
		LAST_SIZE.remove(player);
		ACTIVE_CASTS.remove(player);
	}

	/** Removes both UUID state and the exact transient modifiers from a departing entity. */
	public static void clear(ServerPlayer player) {
		if (player == null) return;
		clear(player.getUUID());
		removeOwnedModifiers(player);
	}

	/** Drops all transient selection state during server shutdown. */
	public static void clearAll() {
		LAST_SIZE.clear();
		ACTIVE_CASTS.clear();
	}
}

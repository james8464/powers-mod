package com.powers.power.crystals;

import com.powers.PowerStatusEffects;
import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.progression.ScaledMagicValues;
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

import java.util.HashMap;
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

	// shrink -0.62 to 38% of normal size, grow +0.75 to 175%; the giant also gets full knockback resistance
	private static final AttributeModifier SHRINK_MODIFIER = new AttributeModifier(
			PowersMod.id("size_shift_shrink"), -0.62, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	private static final AttributeModifier GROW_MODIFIER = new AttributeModifier(
			PowersMod.id("size_shift_grow"), 0.75, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	private static final AttributeModifier ANTI_KNOCKBACK = new AttributeModifier(
			PowersMod.id("size_shift_knockback"), 1.0, AttributeModifier.Operation.ADD_VALUE);

	// Selection and cleanup tokens are runtime-only and are cleared at lifecycle edges.
	private static final Map<UUID, Integer> LAST_SIZE = new HashMap<>();
	private static final Map<UUID, Long> ACTIVE_CASTS = new HashMap<>();
	private static long nextCastToken;

	public SizeShiftAbility() {
		super(PowersMod.id("size_shift"),
				Component.translatable("ability.powers.size_shift"),
				COOLDOWN_TICKS, false, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		int next = LAST_SIZE.getOrDefault(player.getUUID(), 2) == 1 ? 2 : 1;
		LAST_SIZE.put(player.getUUID(), next);

		ServerLevel level = (ServerLevel) player.level();
		ScaledMagicValues values = scaling(player);
		int duration = scaledDuration(player, DURATION_TICKS);
		int potencyTier = values.potencyMultiplier() >= 1.25 ? 1 : 0;
		AttributeInstance scale = player.getAttribute(Attributes.SCALE);
		AttributeInstance knockback = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		removeOwnedModifiers(player);
		if (next == 1) {
			if (scale != null) {
				scale.addOrUpdateTransientModifier(SHRINK_MODIFIER);
			}
			player.addEffect(PowerStatusEffects.hidden(MobEffects.SPEED,
					duration, 3 + potencyTier, true, true));
			player.addEffect(PowerStatusEffects.hidden(MobEffects.JUMP_BOOST,
					duration, 4 + potencyTier, true, true));
			player.addEffect(PowerStatusEffects.hidden(MobEffects.SLOW_FALLING,
					duration, 0, true, true));
			PowerFx.coloredBurst(level, player.position().add(0, 1, 0), 0x00E5FF, 24, 0.8);
			PowerFx.burst(level, player.position().add(0, 1, 0), ParticleTypes.POOF, 20, 0.7, 0.2);
		} else {
			if (scale != null) {
				scale.addOrUpdateTransientModifier(GROW_MODIFIER);
			}
			if (knockback != null) {
				knockback.addOrUpdateTransientModifier(ANTI_KNOCKBACK);
			}
			player.addEffect(PowerStatusEffects.hidden(MobEffects.STRENGTH,
					duration, 3 + potencyTier, true, true));
			player.addEffect(PowerStatusEffects.hidden(MobEffects.RESISTANCE,
					duration, 1, true, true));
			PowerFx.coloredBurst(level, player.position().add(0, 1, 0), 0xFFD600, 30, 1.6);
			PowerFx.burst(level, player.position().add(0, 1, 0), ParticleTypes.POOF, 30, 1.0, 0.3);
		}
		PowerFx.rune(level, player.position(), next == 1 ? 1.15 : 2.15,
				next == 1 ? 0x00E5FF : 0xFFD600, 28, next == 1 ? 0.0 : Math.PI);
		PowerFx.sound(level, player.position(), SoundEvents.PLAYER_HURT_FREEZE, 1.0f, 0.7f);

		long token = ++nextCastToken;
		ACTIVE_CASTS.put(player.getUUID(), token);
		PowersMod.scheduleDelayed(level.getServer(), duration, () -> {
			// An older timer must never remove a newer transformation.
			if (player.isRemoved() || !ACTIVE_CASTS.remove(player.getUUID(), token)) return;
			removeOwnedModifiers(player);
			PowerFx.burst((ServerLevel) player.level(), player.position().add(0, 1, 0),
					ParticleTypes.POOF, 16, 0.8, 0.2);
		});
		return true;
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

	/** Drops all transient selection state during server shutdown. */
	public static void clearAll() {
		LAST_SIZE.clear();
		ACTIVE_CASTS.clear();
	}
}

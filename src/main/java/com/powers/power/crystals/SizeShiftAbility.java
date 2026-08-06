package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Size Shift: the Yellow Crystal's power. You bend the size of your own
 * body: shrink to a speck that slips past every blade, or tower over your
 * enemies and crush them. Each activation alternates to the other size.
 */
public class SizeShiftAbility extends Ability {
	private static final int DURATION_TICKS = 400;
	private static final int COOLDOWN_TICKS = 600;

	private static final AttributeModifier SHRINK_MODIFIER = new AttributeModifier(
			PowersMod.id("size_shift_shrink"), -0.62, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	private static final AttributeModifier GROW_MODIFIER = new AttributeModifier(
			PowersMod.id("size_shift_grow"), 0.75, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	private static final AttributeModifier ANTI_KNOCKBACK = new AttributeModifier(
			PowersMod.id("size_shift_knockback"), 1.0, AttributeModifier.Operation.ADD_VALUE);

	/** The last size used (1 = small, 2 = large); each use alternates. */
	private static final Map<ServerPlayer, Integer> LAST_SIZE = new HashMap<>();

	public SizeShiftAbility() {
		super(PowersMod.id("size_shift"),
				Component.translatable("ability.powers.size_shift"),
				COOLDOWN_TICKS, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		int next = LAST_SIZE.getOrDefault(player, 2) == 1 ? 2 : 1;
		LAST_SIZE.put(player, next);

		ServerLevel level = (ServerLevel) player.level();
		AttributeInstance scale = player.getAttribute(Attributes.SCALE);
		AttributeInstance knockback = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (next == 1) {
			if (scale != null) {
				scale.addOrUpdateTransientModifier(SHRINK_MODIFIER);
			}
			player.addEffect(new MobEffectInstance(MobEffects.SPEED, DURATION_TICKS, 3, true, false));
			player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, DURATION_TICKS, 4, true, false));
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, DURATION_TICKS, 0, true, false));
			PowerFx.coloredBurst(level, player.position().add(0, 1, 0), 0x00E5FF, 24, 0.8);
			PowerFx.burst(level, player.position().add(0, 1, 0), ParticleTypes.POOF, 20, 0.7, 0.2);
		} else {
			if (scale != null) {
				scale.addOrUpdateTransientModifier(GROW_MODIFIER);
			}
			if (knockback != null) {
				knockback.addOrUpdateTransientModifier(ANTI_KNOCKBACK);
			}
			player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, DURATION_TICKS, 3, true, false));
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, DURATION_TICKS, 1, true, false));
			PowerFx.coloredBurst(level, player.position().add(0, 1, 0), 0xFFD600, 30, 1.6);
			PowerFx.burst(level, player.position().add(0, 1, 0), ParticleTypes.POOF, 30, 1.0, 0.3);
		}
		PowerFx.sound(level, player.position(), SoundEvents.PLAYER_HURT_FREEZE, 1.0f, 0.7f);

		ServerPlayer endPlayer = player;
		PowersMod.scheduleDelayed(level.getServer(), DURATION_TICKS, () -> {
			if (endPlayer.getAttribute(Attributes.SCALE) != null) {
				endPlayer.getAttribute(Attributes.SCALE).removeModifier(SHRINK_MODIFIER);
				endPlayer.getAttribute(Attributes.SCALE).removeModifier(GROW_MODIFIER);
			}
			if (endPlayer.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) {
				endPlayer.getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeModifier(ANTI_KNOCKBACK);
			}
			PowerFx.burst((ServerLevel) endPlayer.level(), endPlayer.position().add(0, 1, 0),
					ParticleTypes.POOF, 16, 0.8, 0.2);
		});
		return true;
	}
}

package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class SuperSpeedAbility extends Ability {
	private static final int DURATION = 160;

	public SuperSpeedAbility() {
		super(PowersMod.id("super_speed"),
				Component.translatable("ability.powers.super_speed"),
				300, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		player.addEffect(new MobEffectInstance(MobEffects.SPEED, DURATION, 4, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, DURATION, 2, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, DURATION, 0, false, false));
		com.powers.fx.PowerFx.ring(level, player.position(), 1.1, 0x00E5FF, 18, 0);
		com.powers.fx.PowerFx.spiral(level, player.position().add(0, 0.1, 0), 0.7, 1.8, 0x00E5FF, 18, 0);
		com.powers.fx.PowerFx.burst(level, player.position(),
				net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, 18, 0.7, 0.2);

		com.powers.fx.PowerFx.sound(level, player.position(),
				net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 1.8f);
		return true;
	}
}

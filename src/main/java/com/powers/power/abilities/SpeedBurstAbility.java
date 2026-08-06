package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Speed Burst: a short, explosive burst of super speed in the direction you
 * are looking, with a brief slow fall to glide. Inspired by speedster
 * heroes (Flash, Quicksilver) from superhero mods.
 */
public class SpeedBurstAbility extends Ability {
	public SpeedBurstAbility() {
		super(PowersMod.id("speed_burst"),
				Component.translatable("ability.powers.speed_burst"),
				140, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		var level = (net.minecraft.server.level.ServerLevel) player.level();
		Vec3 dir = player.getLookAngle().normalize();
		Vec3 pos = player.position();
		player.setDeltaMovement(player.getDeltaMovement().add(dir.scale(2.2)));
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, false));
		com.powers.fx.PowerFx.burst(level, pos, net.minecraft.core.particles.ParticleTypes.CLOUD, 12, 0.35, 0.25);
		com.powers.fx.PowerFx.burst(level, pos.add(dir.scale(2.0)), net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, 8, 0.45, 0.25);
		com.powers.fx.PowerFx.sound(level, pos, net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_SHOOT, 1.0f, 1.5f);
		return true;
	}
}

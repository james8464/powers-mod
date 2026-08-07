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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Life Bloom: the Green Crystal's power. Life itself answers your call:
 * every player within twenty blocks is healed to full health, freed of
 * every ailment, and wreathed in regeneration so potent it outlasts
 * any fight
 */
public class LifeBloomAbility extends Ability {
	private static final int COOLDOWN_TICKS = 2400;
	private static final int RADIUS = 20;

	public LifeBloomAbility() {
		super(PowersMod.id("life_bloom"),
				Component.translatable("ability.powers.life_bloom"),
				COOLDOWN_TICKS, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 origin = player.position().add(0, 1, 0);
		// the box reaches 20 blocks out from the caster in every direction
		for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class,
				AABB.ofSize(origin, RADIUS * 2, RADIUS * 2, RADIUS * 2), LivingEntity::isAlive)) {
			if (ally instanceof ServerPlayer orPlayer) {
				orPlayer.removeAllEffects();
				orPlayer.heal(orPlayer.getMaxHealth());
				// 600 ticks = 30 seconds of regen, absorption and saturation, enough to carry anyone through a fight
				orPlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 4, true, false));
				orPlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 3, true, false));
				orPlayer.addEffect(new MobEffectInstance(MobEffects.SATURATION, 600, 0, true, false));
				PowerFx.coloredBurst(level, orPlayer.position().add(0, 1, 0), 0x00C853, 12, 0.6);
			}
		}
		PowerFx.coloredBurst(level, origin, 0x00C853, 40, 2.0);
		PowerFx.burst(level, origin, ParticleTypes.HAPPY_VILLAGER, 30, 2.0, 0.2);
		PowerFx.sound(level, player.position(), SoundEvents.TOTEM_USE, 1.0f, 0.8f);
		return true;
	}
}

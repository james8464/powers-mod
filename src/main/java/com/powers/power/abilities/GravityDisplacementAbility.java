package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

public class GravityDisplacementAbility extends Ability {
	private static final double RADIUS = 8.0;
	private static final int DURATION = 80;

	public GravityDisplacementAbility() {
		super(PowersMod.id("gravity_displacement"),
				Component.translatable("ability.powers.gravity_displacement"),
				300, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		AABB area = AABB.ofSize(player.position(), RADIUS * 2, RADIUS * 2, RADIUS * 2);
		for (LivingEntity target : level.getEntities(EntityTypeTest.forClass(LivingEntity.class), area,
				e -> e.isAlive() && e != player && !AmethystDampening.isDampened(e))) {
			target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, DURATION, 3, false, false));
		}

		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, DURATION + 20, 0, false, false));

		com.powers.fx.PowerFx.burst(level, player.position(),
				net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, 30, RADIUS, 0.05);
		com.powers.fx.PowerFx.sound(level, player.position(),
				net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT, 1.0f, 0.3f);
		return true;
	}
}

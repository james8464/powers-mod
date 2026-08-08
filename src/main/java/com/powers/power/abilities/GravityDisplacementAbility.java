package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.protection.PowerProtection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

/**
 * Gravity Displacement: everyone around you floats helplessly while you
 * drift safely. Lifts enemies into the air and gives you slow falling so
 * you don't come crashing down with them.
 */
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
		double radius = scaledRange(player, RADIUS);
		int duration = scaledDuration(player, DURATION);
		// everyone in an 8-block radius around the player floats up for 4 seconds
		AABB area = AABB.ofSize(player.position(), radius * 2, radius * 2, radius * 2);
		for (LivingEntity target : level.getEntities(EntityTypeTest.forClass(LivingEntity.class), area,
				e -> e.isAlive() && e != player && !AmethystDampening.isDampened(e)
						&& PowerProtection.mayForceMove(player, e))) {
			target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, duration, 3, false, false));
		}

		// lasts a second longer than the enemies' levitation so you drift down safely
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration + 20, 0, false, false));

		com.powers.fx.PowerFx.burst(level, player.position(),
				net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, 30, radius, 0.05);
		com.powers.fx.PowerFx.rune(level, player.position(), radius * 0.55, 0x8C66FF, 28, 0.0);
		com.powers.fx.PowerFx.sound(level, player.position(),
				net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT, 1.0f, 0.3f);
		return true;
	}
}

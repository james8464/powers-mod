package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AbilityArithmetic;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Creates a temporary hearth that heals and feeds permitted nearby allies. */
public class CozyCampfireAbility extends Ability {
	// 10 seconds of warmth
	private static final int DURATION = 200;

	public CozyCampfireAbility() {
		super(PowersMod.id("cozy_campfire"),
				Component.translatable("ability.powers.cozy_campfire"),
				600, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 center = player.position();
		// reach of the cozy aura
		double radius = 6.0;

		// run the first heal tick immediately, then every 5 ticks after
		PowersMod.scheduleDelayed(level.getServer(), 0, new HealTask(level, center, radius, DURATION));

		com.powers.fx.PowerFx.burst(level, center,
				net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, 16, 1.5, 0.1);
		com.powers.fx.PowerFx.burst(level, center,
				net.minecraft.core.particles.ParticleTypes.FLAME, 8, 1.0, 0.05);
		com.powers.fx.PowerFx.sound(level, center,
				SoundEvents.FIRECHARGE_USE, 0.8f, 0.7f);
		return true;
	}

	private static class HealTask implements Runnable {
		private final ServerLevel level;
		private final Vec3 center;
		private final double radius;
		private int remaining;

		HealTask(ServerLevel level, Vec3 center, double radius, int ticks) {
			this.level = level;
			this.center = center;
			this.radius = radius;
			this.remaining = ticks;
		}

		@Override
		public void run() {
			if (remaining <= 0) return;
			// only friendly mobs get healed, never enemies
			AABB area = AABB.ofSize(center, radius * 2, radius * 2, radius * 2);
			for (LivingEntity e : level.getEntities(EntityTypeTest.forClass(LivingEntity.class), area,
					e -> e.isAlive() && !(e instanceof net.minecraft.world.entity.monster.Enemy)
							&& e.position().distanceTo(center) <= radius)) {
				e.heal(2.0f);
				// and players also get a little hunger back
				if (e instanceof net.minecraft.world.entity.player.Player p) {
					p.getFoodData().eat(1, 0.5f);
				}
			}
			com.powers.fx.PowerFx.burst(level, center,
					net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, 2, radius * 0.3, 0.02);
			remaining = AbilityArithmetic.afterPulse(remaining, 5);
			if (remaining > 0) {
				// re-schedule the next tick, ticking every 5 ticks
				PowersMod.scheduleDelayed(level.getServer(), 5, this);
			}
		}
	}
}

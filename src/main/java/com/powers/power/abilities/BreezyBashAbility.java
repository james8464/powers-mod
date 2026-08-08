package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.protection.PowerProtection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Breezy Bash: kick up a gust that hurls everyone nearby skyward, then
// gravity slams them back down a second later.
public class BreezyBashAbility extends Ability {
	public BreezyBashAbility() {
		super(PowersMod.id("breezy_bash"),
				Component.translatable("ability.powers.breezy_bash"),
				400, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();

		// 16-block cube around the player, skipping yourself and shielded targets
		AABB area = AABB.ofSize(player.position(), 16.0, 16.0, 16.0);
		for (LivingEntity target : level.getEntities(
				EntityTypeTest.forClass(LivingEntity.class), area,
				e -> e.isAlive() && e != player && !AmethystDampening.isDampened(e)
						&& PowerProtection.mayForceMove(player, e))) {
			target.setDeltaMovement(0, 1.6, 0);
			target.hurtMarked = true;

			// 18 ticks later, on the way down, slam them into the ground
			PowersMod.scheduleDelayed(level.getServer(), 18, () -> {
				if (target.isAlive() && target.level() == level
						&& PowerProtection.mayForceMove(player, target)
						&& !AmethystDampening.isDampened(target)) {
					target.setDeltaMovement(0, -2.5, 0);
					target.hurtMarked = true;
				}
			});
		}

		com.powers.fx.PowerFx.burst(level, player.position(),
				net.minecraft.core.particles.ParticleTypes.GUST_EMITTER_LARGE, 20, 1.5, 0.2);
		com.powers.fx.PowerFx.sound(level, player.position(),
				SoundEvents.BREEZE_SHOOT, 1.5f, 0.6f);
		return true;
	}
}

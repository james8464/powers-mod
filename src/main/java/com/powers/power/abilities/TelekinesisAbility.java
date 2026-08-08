package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.AmethystDampening;
import com.powers.power.state.PowerEntityState;
import com.powers.protection.PowerProtection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * telekinesis - psychically seize every living thing around you, yank it
 * into the air and fling it away, the way scarlet witch throws enemies
 * around
 */
public class TelekinesisAbility extends Ability {
	public TelekinesisAbility() {
		super(PowersMod.id("telekinesis"),
				Component.translatable("ability.powers.telekinesis"),
				240, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		double range = scaledRange(player, 8.0);
		double force = Math.min(1.35, scaling(player).potencyMultiplier());
		AABB area = AABB.ofSize(player.position(), range * 2, range * 1.5, range * 2);
		// an 8-block reach in all directions, 12 tall, centered on the player
		Vec3 center = player.position();
		for (LivingEntity target : level.getEntities(
				EntityTypeTest.forClass(LivingEntity.class), area,
				e -> e.isAlive() && e != player && !AmethystDampening.isDampened(e)
						&& PowerProtection.mayForceMove(player, e))) {
			Vec3 toward = center.subtract(target.position());
			double horizontal = toward.horizontalDistance();
			// right on top of the player the fling direction is undefined, skip them
			if (horizontal < 0.01) {
				continue;
			}
			// launch the target up and away: 2.2 blocks/s outward, 0.7 upward
			Vec3 fling = toward.multiply(1, 0, 1).normalize().scale(2.2 * force).add(0, 0.7 * force, 0);
			target.setDeltaMovement(target.getDeltaMovement().add(fling));
			// flag the velocity change so the client replays it and the throw looks instant
			target.hurtMarked = true;
			com.powers.fx.PowerFx.beam(level, center.add(0, 1.2, 0), target.position().add(0, 1, 0),
					net.minecraft.core.particles.ParticleTypes.ENCHANT, 8);
			com.powers.fx.PowerFx.coloredBurst(level, target.position().add(0, 1, 0), 0x9C27B0, 6, 0.4);
		}
		int intercepted = 0;
		for (Projectile projectile : level.getEntities(EntityTypeTest.forClass(Projectile.class), area,
				x -> x.isAlive() && x.getOwner() != player)) {
			if (intercepted >= 16) break;
			if (!PowerEntityState.tryReflect(projectile, 1)) continue;
			intercepted++;
			projectile.setOwner(player);
			projectile.setDeltaMovement(player.getLookAngle().normalize().scale(1.8 * force));
			projectile.hurtMarked = true;
			com.powers.fx.PowerFx.rune(level, projectile.position(), 0.45, 0xC27CFF, 10, intercepted);
		}
		com.powers.fx.PowerFx.coloredBurst(level, center.add(0, 1.2, 0), 0x9C27B0, 20, 0.8);
		com.powers.fx.PowerFx.rune(level, center, range * 0.45, 0xC27CFF, 24, player.tickCount * 0.08);
		com.powers.fx.PowerFx.sound(level, center, net.minecraft.sounds.SoundEvents.EVOKER_CAST_SPELL, 1.0f, 0.9f);
		return true;
	}
}

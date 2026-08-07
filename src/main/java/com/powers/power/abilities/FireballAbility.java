package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.phys.Vec3;

/**
 * Fireball: a fireball is summoned hovering in front of you and just floats
 * there; punch it (any melee click) and it shoots off the way you're looking,
 * the same deflection mechanic as a ghast fireball. Fades if never hit.
 */
public class FireballAbility extends Ability {
	// 12 seconds to catch fire
	private static final int DESPAWN_TICKS = 240;

	public FireballAbility() {
		super(PowersMod.id("fireball"),
				Component.translatable("ability.powers.fireball"),
				0, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();

		// step outward from 1.5 to 5 blocks until clear air, so the fireball
		// never spawns inside a wall and suffocates
		Vec3 pos = null;
		for (double distance = 1.5; distance <= 5.0; distance += 0.5) {
			Vec3 candidate = eye.add(look.scale(distance));
			if (!level.getBlockState(BlockPos.containing(candidate)).isSolid()) {
				pos = candidate;
				break;
			}
		}
		if (pos == null) {
			// boxed in, fall back to right in front of the face
			pos = eye.add(look.scale(1.5));
		}

		LargeFireball fireball = new LargeFireball(level, player, Vec3.ZERO, 1);
		fireball.setPos(pos);
		level.addFreshEntity(fireball);

		// the floating fireball fades away after 12 seconds if nobody hits it
		PowersMod.scheduleDelayed(level.getServer(), DESPAWN_TICKS, () -> {
			if (!fireball.isRemoved()) {
				fireball.discard();
			}
		});

		com.powers.fx.PowerFx.burst(level, pos, ParticleTypes.FLAME, 10, 0.2, 0.05);
		com.powers.fx.PowerFx.burst(level, pos, ParticleTypes.SMOKE, 6, 0.15, 0.03);
		com.powers.fx.PowerFx.sound(level, pos, SoundEvents.FIRECHARGE_USE, 1.0f, 1.1f);
		return true;
	}
}

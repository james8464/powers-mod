package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.progression.PowerScalingService;
import com.powers.power.Ability;
import com.powers.power.PowerDamage;
import com.powers.power.AmethystDampening;
import com.powers.protection.PowerProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Ground Slam: a hulk-style ground pound. The impact detonates the ground
 * beneath you, carving a crater and hurling everything nearby.
 */
public class GroundSlamAbility extends Ability {
	public GroundSlamAbility() {
		super(PowersMod.id("ground_slam"),
				Component.translatable("ability.powers.ground_slam"),
				200, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		double radius = scaledRange(player, 5.0);
		PowerFx.burst(level, player.position().add(0, 0.5, 0), net.minecraft.core.particles.ParticleTypes.EXPLOSION, 28, 1.5, 0.2);
		PowerFx.ring(level, player.position().add(0, 0.1, 0), 3.5, 0xFF8A00, 28, 0.0);
		PowerFx.sound(level, player.position(), SoundEvents.GENERIC_EXPLODE.value(), 1.0f, 0.8f);

		carveCrater(player, level, BlockPos.containing(player.getX(), player.getY() - 0.5, player.getZ()));

		DamageSource source = PowerDamage.source(player);
		// shockwave hits everything in a 5-block radius around the player
		AABB area = AABB.ofSize(player.position(), radius * 2, 6.0, radius * 2);
		for (LivingEntity target : level.getEntities(
			EntityTypeTest.forClass(LivingEntity.class), area,
			e -> e.isAlive() && e != player && !AmethystDampening.isDampened(e)
					&& PowerProtection.mayHarm(player, e))) {
			// damage scales with the player's skill level
			target.hurtServer(level, source, PowerScalingService.damage(player, "ground_slam", 6.0f));
			// fling them away from the player and send up smoke
			Vec3 away = target.position().subtract(player.position()).normalize();
			if (PowerProtection.mayForceMove(player, target)) {
				target.knockback(Math.min(2.1, 1.6 * scaling(player).potencyMultiplier()),
						away.x, away.z, source, 1.0f);
			}
			PowerFx.burst(level, target.position().add(0, target.getBbHeight() * 0.5, 0),
					net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, 6, 0.5, 0.02);
		}
		return true;
	}

	/** Carves a bowl-shaped crater from soft ground; the damage is dealt once, by the shockwave. */
	private static void carveCrater(ServerPlayer player, ServerLevel level, BlockPos feet) {
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				if (dx * dx + dz * dz > 9) {
					// circle check keeps the crater round, radius 3
					continue;
				}
				// dig down up to 2 blocks per column
				for (int dy = 0; dy >= -2; dy--) {
					BlockPos pos = feet.offset(dx, dy, dz);
					BlockState state = level.getBlockState(pos);
					// skip fluids, bedrock and anything nearly unbreakable
					if (state.isAir() || state.is(Blocks.BEDROCK) || state.is(Blocks.WATER)
							|| state.is(Blocks.LAVA) || state.getDestroySpeed(level, pos) > 50.0f) {
						continue;
					}
					if (!PowerProtection.mayAffectBlock(player, level, pos)) continue;
					level.destroyBlock(pos, true, player);
					break;
				}
			}
		}
	}
}

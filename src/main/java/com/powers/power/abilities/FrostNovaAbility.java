package com.powers.power.abilities;

import com.powers.PowerStatusEffects;
import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.power.PowerDamage;
import com.powers.power.AmethystDampening;
import com.powers.protection.PowerProtection;
import com.powers.spell.SpellFieldManager;
import com.powers.util.BoundedEntityCandidates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

/**
 * Frost Nova: an icy blast that freezes the area solid. Nearby water turns
 * to frosted ice on contact while enemies take damage and get chilled.
 */
public class FrostNovaAbility extends Ability {
	public FrostNovaAbility() {
		super(PowersMod.id("frost_nova"),
				Component.translatable("ability.powers.frost_nova"),
				300, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		BlockPos center = player.blockPosition();
		int radius = Math.max(4, (int) Math.floor(scaledRange(player, 4.0)));
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > radius * radius) {
					// circle check so the blast stays round instead of square
					continue;
				}
				// scan a vertical slice from waist height up to eye level
				for (int dy = -1; dy <= 2; dy++) {
					BlockPos pos = center.offset(dx, dy, dz);
					if (level.getFluidState(pos).isSourceOfType(Fluids.WATER)
							&& PowerProtection.mayAffectBlock(player, level, pos)) {
						level.setBlock(pos, Blocks.FROSTED_ICE.defaultBlockState(), 3);
					}
				}
			}
		}

		com.powers.fx.PowerFx.burst(level, player.position().add(0, 1, 0),
				net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, 30, 3.0, 0.35);
		com.powers.fx.PowerFx.sound(level, player.position(),
				net.minecraft.sounds.SoundEvents.PLAYER_HURT_FREEZE, 1.0f, 1.0f);

		// catch everything within a 6-block radius around the player
		double entityRadius = scaledRange(player, 6.0);
		AABB area = AABB.ofSize(player.position(), entityRadius * 2, 8.0, entityRadius * 2);
		for (LivingEntity target : BoundedEntityCandidates.living(level, area, 160,
				e -> e.isAlive() && e != player && !AmethystDampening.isDampened(e)
						&& PowerProtection.mayHarm(player, e)
						&& !SpellFieldManager.isSanctuaryProtected(level, e))) {
			// 4 damage plus a heavy slow for 6 seconds
			target.hurtServer(level, PowerDamage.source(player), scaledPotency(player, 4.0f));
			target.addEffect(PowerStatusEffects.hidden(MobEffects.SLOWNESS,
					scaledDuration(player, 120), 2, false, true));
		}
		return true;
	}
}

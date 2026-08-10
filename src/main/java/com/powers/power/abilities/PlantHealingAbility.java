package com.powers.power.abilities;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Plant Healing: quickens the growth of the plant you're looking at, like
 * a burst of bonemeal energy. Does nothing and refunds energy if there's
 * nothing growable in sight.
 */
public class PlantHealingAbility extends Ability {
	public PlantHealingAbility() {
		super(PowersMod.id("plant_healing_acceleration"),
				Component.translatable("ability.powers.plant_healing_acceleration"), 0, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		HitResult hit = player.pick(scaledRange(player, 12.0), 0.0f, false);
		if (!(hit instanceof BlockHitResult blockHit)) return false;
		// bonemeal the plant that was actually hit, not the empty block beyond it
		var pos = blockHit.getBlockPos();
		var state = level.getBlockState(pos);
		// some plants can't be bonemealed (fully grown, for example)
		if (!(state.getBlock() instanceof BonemealableBlock growable)
				|| !growable.isValidBonemealTarget(level, pos, state)) return false;
		growable.performBonemeal(level, level.getRandom(), pos, state);
		if (scaledPotency(player, 1.0f) >= 1.25f) {
			var updated = level.getBlockState(pos);
			if (updated.getBlock() instanceof BonemealableBlock secondGrowth
					&& secondGrowth.isValidBonemealTarget(level, pos, updated)) {
				secondGrowth.performBonemeal(level, level.getRandom(), pos, updated);
			}
		}
		Vec3 center = Vec3.atCenterOf(pos);
		com.powers.fx.PowerFx.ring(level, center, 1.2, 0x66FF66, 16, 0);
		com.powers.fx.PowerFx.burst(level, center,
				com.powers.fx.PowerFx.dust(0x66FF66, 1.0F), 16, 0.5, 0.0);
		com.powers.fx.PowerFx.sound(level, center,
				net.minecraft.sounds.SoundEvents.BONE_MEAL_USE, 0.8f, 1.3f);
		com.powers.fx.PowerFx.spiral(level, center, 0.7, 1.8, 0x9AF59A, 14, player.tickCount * 0.1);
		return true;
	}
}

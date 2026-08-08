package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.fx.PowerFx;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import com.powers.protection.PowerProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * creativity manifestation - the orange crystal's power of creation: conjure
 * a glowing orange creation chamber out of thin air at the block you're
 * looking at, wherever you want it
 */
public class CreativityManifestationAbility extends Ability {
	private static final double BASE_REACH = 16.0;
	private static final int COOLDOWN_TICKS = 200;

	public CreativityManifestationAbility() {
		super(PowersMod.id("creativity_manifestation"),
				Component.translatable("ability.powers.creativity_manifestation"), COOLDOWN_TICKS, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel level = (ServerLevel) player.level();
		HitResult hit = player.pick(scaledRange(player, BASE_REACH), 0.0f, false);
		if (!(hit instanceof BlockHitResult blockHit)) return false;
		BlockPos center = blockHit.getBlockPos().relative(blockHit.getDirection());
		BlockState frame = Blocks.CONCRETE.pick(DyeColor.ORANGE).defaultBlockState();
		BlockState glass = Blocks.STAINED_GLASS.pick(DyeColor.ORANGE).defaultBlockState();
		int placed = 0;
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
					BlockPos pos = center.offset(dx, 0, dz);
					if (level.getBlockState(pos).isAir() && PowerProtection.mayAffectBlock(player, level, pos)) {
						level.setBlockAndUpdate(pos, frame);
						placed++;
					}
				}
			}
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos pos = center.offset(dx, 1, dz);
				if (level.getBlockState(pos).isAir() && PowerProtection.mayAffectBlock(player, level, pos)) {
					level.setBlockAndUpdate(pos, glass);
					placed++;
				}
			}
		}
		BlockPos light = center.above(2);
		if (level.getBlockState(light).isAir() && PowerProtection.mayAffectBlock(player, level, light)) {
			level.setBlockAndUpdate(light, Blocks.GLOWSTONE.defaultBlockState());
			placed++;
		}
		if (placed == 0) return false;
		Vec3 centerVec = Vec3.atCenterOf(center);
		PowerFx.ring(level, centerVec, 3.2, 0xFF9800, 28, 0);
		PowerFx.spiral(level, centerVec, 2.2, 2.5, 0xFF9800, 28, 0);
		PowerFx.rune(level, centerVec, 2.7, 0xFFD180, 32, Math.PI / 8);
		PowerFx.sound(level, centerVec,
				net.minecraft.sounds.SoundEvents.EVOKER_CAST_SPELL, 1.0f, 1.2f);
		return true;
	}
}

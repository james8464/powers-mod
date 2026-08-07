package com.powers.power.crystals;

import com.powers.PowersMod;
import com.powers.player.PlayerPowers;
import com.powers.power.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

public class MiddleworldAbility extends Ability {
	public MiddleworldAbility() {
		super(PowersMod.id("middleworld"),
				Component.translatable("ability.powers.middleworld"),
				2400, false);
	}

	@Override
	public boolean activate(ServerPlayer player, PlayerPowers.PlayerPowersData data) {
		ServerLevel targetLevel = player.level().getServer().getLevel(
				net.minecraft.resources.ResourceKey.create(
						net.minecraft.core.registries.Registries.DIMENSION,
						PowersMod.id("middleworld")));
		if (targetLevel == null) return false;

		Vec3 dest = findSafePos(targetLevel, 8, 8, 8);
		player.teleport(new TeleportTransition(targetLevel, dest, Vec3.ZERO,
				player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
		return true;
	}

	private Vec3 findSafePos(ServerLevel level, int x, int y, int z) {
		// Scan top-down so the traveller lands on the surface instead of a cave.
		int startY = level.getMaxY() - 2;
		for (int dy = 0; dy < level.getHeight() - 2; dy++) {
			int cy = startY - dy;
			BlockPos feet = new BlockPos(x, cy, z);
			BlockPos head = new BlockPos(x, cy + 1, z);
			BlockState fb = level.getBlockState(feet);
			BlockState hb = level.getBlockState(head);
			if (!fb.isSolid() && !hb.isSolid() && !level.getBlockState(new BlockPos(x, cy - 1, z)).isAir()) {
				return new Vec3(x + 0.5, cy, z + 0.5);
			}
		}
		return new Vec3(x + 0.5, level.getMaxY() - 1, z + 0.5);
	}
}

package com.powers.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * INERT. The Rainbow Crystal no longer holds the power re-roll mechanic —
 * it is a reserved artifact waiting for its own dedicated purpose.
 */
public class RainbowCrystalItem extends Item {
	public RainbowCrystalItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		// No purpose yet — right-clicking does nothing for now.
		return InteractionResult.SUCCESS;
	}
}

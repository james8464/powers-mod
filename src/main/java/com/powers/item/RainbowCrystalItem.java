package com.powers.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * inert placeholder artifact. the re-roll mechanic is gone and the crystal
 * is waiting for its own dedicated purpose
 */
public class RainbowCrystalItem extends Item {
	public RainbowCrystalItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		// right click does nothing yet - reserved artifact
		return InteractionResult.SUCCESS;
	}
}

package com.powers.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * TEMPORARILY INERT. The Rainbow Crystal's re-roll functionality has been
 * disabled: the client-side power selection screen (previously opened through
 * {@code ItemEvents.USE}) and the server acknowledgement were removed. The
 * item, its recipe and its advancement quests remain so it is still
 * collectible/craftable, but right-clicking it does nothing.
 */
public class RainbowCrystalItem extends Item {
	public RainbowCrystalItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		// TODO(rainbow crystal): re-enable the power re-roll flow here.
		// Previously this opened the client power selection screen and sent
		// the re-roll packets. For now it does nothing.
		return InteractionResult.SUCCESS;
	}
}

package com.powers.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Pure-darkness blade whose server-validated menu channels every registered power. */
public final class ShadowSwordItem extends Item {
	public ShadowSwordItem(Properties properties) {
		super(properties.stacksTo(1).fireResistant());
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		if (!level.isClientSide() && user instanceof ServerPlayer player) {
			if (player.isCrouching()) {
				ShadowSwordPowerManager.openMenu(player);
			} else {
				ShadowSwordPowerManager.activateSelected(player);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable(getDescriptionId())
				.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD);
	}

}

package com.powers.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Opens the power selection screen. The screen itself is opened on the client
 * through {@code ItemEvents.USE} (registered in the client entrypoint), since
 * common code cannot reference client classes; the server just acknowledges.
 */
public class RainbowCrystalItem extends Item {
	public RainbowCrystalItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (user instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendSystemMessage(Component.translatable("message.powers.choose_power"));
		}
		return InteractionResult.SUCCESS;
	}
}

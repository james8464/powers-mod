package com.powers.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** A lore-bearing book hook; chapter content can be added without changing registration. */
public class GrimoireItem extends Item {
	private final String key;

	public GrimoireItem(Properties properties, String key) {
		super(properties.stacksTo(1));
		this.key = key;
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		if (!level.isClientSide() && user instanceof ServerPlayer player) {
			player.sendSystemMessage(Component.translatable("grimoire.powers.placeholder", key));
		}
		return InteractionResult.SUCCESS;
	}
}

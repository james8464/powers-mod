package com.powers.item;

import com.powers.player.PlayerPowers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** A reusable rune focus that channels a modest amount of energy back to its bearer. */
public class RuneItem extends Item {
	public RuneItem(Properties properties) {
		super(properties.stacksTo(16));
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		if (!level.isClientSide() && user instanceof ServerPlayer player) {
			PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
			if (data.regenerateEnergy(100)) {
				player.sendSystemMessage(Component.translatable("rune.powers.channelled"));
			} else {
				player.sendSystemMessage(Component.translatable("rune.powers.full"));
			}
		}
		return InteractionResult.SUCCESS;
	}
}

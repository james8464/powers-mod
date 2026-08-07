package com.powers.item;

import com.powers.player.PlayerPowers;
import com.powers.util.PowerMessages;
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
				PowerMessages.send(player, "rune.powers.channelled", 4);
			} else {
				PowerMessages.send(player, "rune.powers.full", 4);
			}
		}
		return InteractionResult.SUCCESS;
	}
}

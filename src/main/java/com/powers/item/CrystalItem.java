package com.powers.item;

import com.powers.power.crystals.CrystalPowerRegistry;
import com.powers.util.PowerMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * a crystal charged with an ultra-powerful, game-changing ability: right
 * click unleashes it. crystal powers are a tier above regular steve powers,
 * never assigned randomly - they must be crafted and held
 */
public class CrystalItem extends Item {
	public CrystalItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		// server side only; an unbound crystal stays dormant and says so
		if (!level.isClientSide() && user instanceof ServerPlayer serverPlayer) {
			if (CrystalPowerRegistry.get(this) == null) {
				PowerMessages.send(serverPlayer, "crystal.powers.dormant", 3);
				return InteractionResult.SUCCESS;
			}
			CrystalPowerRegistry.tryActivate(serverPlayer, this);
		}
		return InteractionResult.SUCCESS;
	}
}

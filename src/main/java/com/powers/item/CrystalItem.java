package com.powers.item;

import com.powers.power.crystals.CrystalPowerRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A crystal charged with an ultra-powerful, game-changing ability. Right
 * clicking unleashes it. Crystal powers are a tier above regular Steve
 * powers: they are never assigned randomly and can never be rolled with the
 * Rainbow Crystal - they must be crafted and held.
 */
public class CrystalItem extends Item {
	public CrystalItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		if (!level.isClientSide() && user instanceof ServerPlayer serverPlayer) {
			if (CrystalPowerRegistry.get(this) == null) {
				return InteractionResult.SUCCESS;
			}
			if (CrystalPowerRegistry.tryActivate(serverPlayer, this)) {
				return InteractionResult.SUCCESS;
			}
			serverPlayer.sendSystemMessage(Component.translatable("crystal.powers.unavailable"));
		}
		return InteractionResult.SUCCESS;
	}
}

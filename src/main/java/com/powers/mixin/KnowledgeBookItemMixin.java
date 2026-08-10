package com.powers.mixin;

import com.powers.network.KnowledgePackets;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.KnowledgeBookItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces vanilla's consumable recipe grant with the persistent Knowledge Book UI. */
@Mixin(KnowledgeBookItem.class)
public abstract class KnowledgeBookItemMixin {
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void powers$openKnowledgeBook(Level level, Player player, InteractionHand hand,
			CallbackInfoReturnable<InteractionResult> callback) {
		if (player instanceof ServerPlayer serverPlayer) KnowledgePackets.open(serverPlayer);
		callback.setReturnValue(level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER);
	}
}

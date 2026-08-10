package com.powers.mixin;

import com.powers.player.RankDisplayData;
import com.powers.player.RankNameFormatter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds the same synchronised rank title to the vanilla tab-list name. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerRankDisplayMixin {
	@Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
	private void powers$decorateTabName(CallbackInfoReturnable<Component> cir) {
		ServerPlayer player = (ServerPlayer) (Object) this;
		Component vanillaName = cir.getReturnValue();
		if (vanillaName == null) {
			vanillaName = PlayerTeam.formatNameForTeam(player.getTeam(), player.getName());
		}
		Component prefix = ((RankDisplayData) player).powers$getRankPrefix();
		cir.setReturnValue(RankNameFormatter.decorate(prefix, vanillaName));
	}
}

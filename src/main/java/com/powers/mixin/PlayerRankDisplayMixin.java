package com.powers.mixin;

import com.powers.player.RankDisplayData;
import com.powers.player.RankNameFormatter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Synchronises rank titles as ordinary player entity data. This makes the same
 * styled prefix appear in chat and above remote players without consuming a
 * scoreboard team or relying on a server-only custom name.
 */
@Mixin(Player.class)
public abstract class PlayerRankDisplayMixin implements RankDisplayData {
	@Unique
	private static final EntityDataAccessor<Component> POWERS_RANK_PREFIX =
			SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPONENT);

	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void powers$defineRankPrefix(SynchedEntityData.Builder builder, CallbackInfo ci) {
		builder.define(POWERS_RANK_PREFIX, Component.empty());
	}

	@Override
	public Component powers$getRankPrefix() {
		return ((Player) (Object) this).getEntityData().get(POWERS_RANK_PREFIX);
	}

	@Override
	public void powers$setRankPrefix(Component prefix) {
		((Player) (Object) this).getEntityData().set(POWERS_RANK_PREFIX, prefix);
	}

	@Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
	private void powers$decorateDisplayName(CallbackInfoReturnable<Component> cir) {
		cir.setReturnValue(RankNameFormatter.decorate(
				com.powers.config.PowersConfigLoader.get().rankPrefixesEnabled(),
				powers$getRankPrefix(), cir.getReturnValue()));
	}
}

package com.powers.mixin;

import com.powers.player.PlayerPowers;
import com.powers.player.SkillSystem;
import com.powers.realm.RealmPortalRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.EndGatewayBlock;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents Nether, End, and gateway blocks from bypassing mindscape confinement. */
@Mixin({NetherPortalBlock.class, EndPortalBlock.class, EndGatewayBlock.class})
public abstract class RealmPortalMixin {
	@Inject(method = "getPortalDestination", at = @At("HEAD"), cancellable = true)
	private void powers$enforceRealmDeparture(ServerLevel level, Entity entity, BlockPos pos,
			CallbackInfoReturnable<TeleportTransition> cir) {
		if (!(entity instanceof ServerPlayer player)) return;
		PlayerPowers.PlayerPowersData data = PlayerPowers.get(player);
		if (!RealmPortalRules.mayDepart(level.dimension().identifier().toString(),
				SkillSystem.hasDarknessTag(player), data.skillLevel(), data.darknessLevel())) {
			cir.setReturnValue(null);
		}
	}
}

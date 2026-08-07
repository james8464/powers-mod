package com.powers.mixin;

import com.powers.PowersItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops hostile mobs (zombies, piglins, hoglins, ...) from picking up dropped crystals. */
@Mixin(Mob.class)
public abstract class MobMixin {
	@Inject(method = "pickUpItem", at = @At("HEAD"), cancellable = true)
	private void powers$preventCrystalPickup(ServerLevel level, ItemEntity itemEntity, CallbackInfo ci) {
		if (PowersItems.isCrystal(itemEntity.getItem())) {
			ci.cancel();
		}
	}
}

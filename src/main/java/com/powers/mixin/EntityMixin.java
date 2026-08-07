package com.powers.mixin;

import com.powers.PowersItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps dropped crystals alive through {@code /kill}: {@code kill} is declared
 * on {@link Entity}, so it cannot be injected from an {@link ItemEntity} mixin.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "kill", at = @At("HEAD"), cancellable = true)
	private void powers$protectCrystalFromKill(ServerLevel level, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (self instanceof ItemEntity itemEntity && PowersItems.isCrystal(itemEntity.getItem())) {
			ci.cancel();
		}
	}
}

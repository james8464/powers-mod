package com.powers.mixin;

import com.powers.PowersItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes dropped crystal items indestructible: they never despawn, never burn
 * (the fire resistance already comes from the item property), take no damage
 * (lightning, explosions, fire, /kill via damage) and survive {@code /kill @e}.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void powers$keepCrystalAlive(CallbackInfo ci) {
		ItemEntity self = (ItemEntity) (Object) this;
		if (!PowersItems.isCrystal(self.getItem())) {
			return;
		}
		self.setUnlimitedLifetime();
		if (self.getY() < self.level().getMinY() - 64) {
			self.setPos(self.getX(), self.level().getMinY() + 2, self.getZ());
		}
	}

	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void powers$protectCrystalFromDamage(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> cir) {
		if (PowersItems.isCrystal(((ItemEntity) (Object) this).getItem())) {
			cir.setReturnValue(false);
		}
	}
}

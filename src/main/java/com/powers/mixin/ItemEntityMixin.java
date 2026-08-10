package com.powers.mixin;

import com.powers.PowersItems;
import com.powers.item.MythicArtifactItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * makes dropped crystals indestructible: they never despawn, never burn
 * (the item property handles fire), take no damage and survive /kill
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void powers$keepCrystalAlive(CallbackInfo ci) {
		ItemEntity self = (ItemEntity) (Object) this;
		if (!protectedItem(self)) {
			return;
		}
		// crystals never despawn on their own
		self.setUnlimitedLifetime();
		// a crystal that falls out of the world is held at the bottom instead of being deleted
		if (self.getY() < self.level().getMinY() - 64) {
			self.setPos(self.getX(), self.level().getMinY() + 2, self.getZ());
		}
	}

	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	// crystals ignore all damage: lightning, explosions, fire, you name it
	private void powers$protectCrystalFromDamage(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> cir) {
		if (protectedItem((ItemEntity) (Object) this)) {
			cir.setReturnValue(false);
		}
	}

	private static boolean protectedItem(ItemEntity entity) {
		return PowersItems.isCrystal(entity.getItem())
				|| entity.getItem().getItem() instanceof MythicArtifactItem;
	}
}

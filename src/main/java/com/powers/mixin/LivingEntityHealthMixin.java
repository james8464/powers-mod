package com.powers.mixin;

import com.powers.spell.BloodReadingHistoryService;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records only the effective amount restored by server-side healing. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHealthMixin {
	@Unique private float powers$healthBeforeHeal;

	@Inject(method = "heal", at = @At("HEAD"))
	private void powers$captureHealth(float amount, CallbackInfo ci) {
		powers$healthBeforeHeal = ((LivingEntity) (Object) this).getHealth();
	}

	@Inject(method = "heal", at = @At("TAIL"))
	private void powers$recordHealing(float amount, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		BloodReadingHistoryService.recordHealing(self, self.getHealth() - powers$healthBeforeHeal);
	}
}

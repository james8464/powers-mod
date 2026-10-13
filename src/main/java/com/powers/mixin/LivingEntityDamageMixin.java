package com.powers.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.powers.power.damage.LayeredDamageLedger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Accounts actual health removed across every server damage return, including death. */
@Mixin(LivingEntity.class)
abstract class LivingEntityDamageMixin {
	@Inject(method = "hurtServer", at = @At("HEAD"))
	private void powers$captureHealthBeforeDamage(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> callback,
			@Share("healthBefore") LocalFloatRef healthBefore) {
		healthBefore.set(((LivingEntity) (Object) this).getHealth());
	}

	@Inject(method = "hurtServer", at = @At("RETURN"))
	private void powers$recordActualHealthRemoved(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> callback,
			@Share("healthBefore") LocalFloatRef healthBefore) {
		LayeredDamageLedger.global().recordHealthTransition(
				healthBefore.get(), ((LivingEntity) (Object) this).getHealth());
	}
}

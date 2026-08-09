package com.powers.mixin;

import com.powers.power.abilities.FireballAbility;
import com.powers.power.state.PowerEntityState;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Routes only POWERS-owned fireballs through the controlled Cinderheart impact. */
@Mixin(LargeFireball.class)
public abstract class LargeFireballMixin {
	@Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
	private void powers$resolveOwnedImpact(HitResult hit, CallbackInfo ci) {
		LargeFireball fireball = (LargeFireball) (Object) this;
		if (!PowerEntityState.isPowerProjectile(fireball)) return;
		ci.cancel();
		FireballAbility.resolveImpact(fireball, hit);
	}
}

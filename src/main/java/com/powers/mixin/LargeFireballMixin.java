package com.powers.mixin;

import com.powers.power.AmethystDampening;
import com.powers.power.PowerDamage;
import com.powers.power.state.PowerEntityState;
import com.powers.protection.PowerProtection;
import com.powers.progression.PowerScalingService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Converts only POWERS-owned fireballs to dedicated, protection-aware damage. */
@Mixin(LargeFireball.class)
public abstract class LargeFireballMixin {
	@Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
	private void powers$applyOwnedDamage(EntityHitResult hit, CallbackInfo ci) {
		LargeFireball fireball = (LargeFireball) (Object) this;
		if (!PowerEntityState.isPowerProjectile(fireball)) return;
		ci.cancel();
		if (!(fireball.getOwner() instanceof ServerPlayer caster)
				|| !(hit.getEntity() instanceof LivingEntity target)
				|| !(target.level() instanceof ServerLevel level)
				|| AmethystDampening.isDampened(target)
				|| !PowerProtection.mayHarm(caster, target)) return;
		target.hurtServer(level, PowerDamage.source(caster),
				PowerScalingService.damage(caster, "fireball", 6.0f));
		target.igniteForSeconds(Math.max(1,
				PowerScalingService.duration(caster, "fireball", 60) / 20));
	}
}

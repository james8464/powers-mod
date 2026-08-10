package com.powers.mixin;

import com.powers.power.abilities.FireballAbility;
import com.powers.power.state.PowerEntityState;
import com.powers.magic.runtime.PhysicalMagicPresences;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Enforces the finite launch and reflection budget of owned Cinderhearts. */
@Mixin(Projectile.class)
public abstract class ProjectileDeflectionMixin {
	/** Only projectiles can currently own moving magic presences; avoid touching every entity tick. */
	@Inject(method = "tick", at = @At("TAIL"))
	private void powers$movePhysicalMagicPresence(CallbackInfo callback) {
		PhysicalMagicPresences.move((Projectile) (Object) this);
	}

	@Inject(method = "deflect", at = @At("HEAD"), cancellable = true)
	private void powers$authorizeCinderheartDeflection(ProjectileDeflection deflection,
			Entity deflectingEntity, EntityReference<Entity> newOwner, boolean byAttack,
			CallbackInfoReturnable<Boolean> cir) {
		Projectile projectile = (Projectile) (Object) this;
		if (!(projectile instanceof LargeFireball fireball)
				|| !PowerEntityState.isPowerProjectile(fireball)) return;
		if (!FireballAbility.allowDeflection(fireball, deflectingEntity, byAttack)) {
			cir.setReturnValue(false);
		}
	}
}

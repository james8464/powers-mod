package com.powers.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Temporary diagnostic tracing for the INT-008 full-suite health mutation. */
@Mixin(LivingEntity.class)
public abstract class DiagnosticLivingEntityHealthMixin {
	@Inject(method = "setHealth", at = @At("HEAD"))
	private void powers$tracePlayerHealthDrop(float health, CallbackInfo callback) {
		if ((Object) this instanceof ServerPlayer player && health < player.getHealth()) {
			System.err.println("INT008_HEALTH_DROP uuid=" + player.getUUID()
					+ " name=" + player.getGameProfile().name()
					+ " from=" + player.getHealth() + " to=" + health
					+ " position=" + player.position()
					+ " mode=" + player.gameMode.getGameModeForPlayer());
			new Throwable("INT008 health drop call site").printStackTrace(System.err);
		}
	}
}

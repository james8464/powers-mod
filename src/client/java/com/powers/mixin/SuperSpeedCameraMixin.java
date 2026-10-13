package com.powers.mixin;

import com.powers.client.ClientSuperSpeedComfort;
import com.powers.client.fx.FxAccessibility;
import com.powers.visual.SuperSpeedCameraRules;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies bounded local FOV comfort without changing authoritative movement. */
@Mixin(Camera.class)
public abstract class SuperSpeedCameraMixin {
	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void powers$applyOverdriveComfort(float partialTick,
			CallbackInfoReturnable<Float> callback) {
		if (!ClientSuperSpeedComfort.active()) return;
		Minecraft client = Minecraft.getInstance();
		callback.setReturnValue(SuperSpeedCameraRules.resolveFov(callback.getReturnValue(),
				client.options.fov().get(), true, FxAccessibility.reducedMotion(client),
				client.options.fovEffectScale().get()));
	}
}

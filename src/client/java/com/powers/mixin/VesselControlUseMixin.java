package com.powers.mixin;

import com.powers.client.VesselControlClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Turns right-click into an authenticated body return while controlling a remote vessel. */
@Mixin(Minecraft.class)
public abstract class VesselControlUseMixin {
	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
	private void powers$returnFromRemoteControl(CallbackInfo callback) {
		if (VesselControlClient.requestRelease()) callback.cancel();
	}
}

package com.powers.mixin;

import com.powers.PowersMod;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes the 26.2 overworld sky disc an unbroken white field in the Light Realm. */
@Mixin(SkyRenderer.class)
abstract class LightRealmSkyMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void powers$extractWhiteLightRealmSky(ClientLevel level, float partialTick,
			Camera camera, SkyRenderState state, CallbackInfo info) {
		if (!level.dimension().identifier().equals(PowersMod.id("light_realm"))) return;
		state.skybox = DimensionType.Skybox.OVERWORLD;
		state.skyColor = 0xFFFFFFFF;
		state.sunriseAndSunsetColor = 0;
		state.rainBrightness = 0.0f;
		state.starBrightness = 0.0f;
		state.shouldRenderDarkDisc = false;
	}
}

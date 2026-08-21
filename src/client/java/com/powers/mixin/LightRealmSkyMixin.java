package com.powers.mixin;

import com.powers.PowersMod;
import com.powers.client.fx.FxAccessibility;
import com.powers.client.realm.LightRealmSkyClientState;
import com.powers.visual.LightRealmSkyProfile;
import com.powers.visual.LightRealmSkyRules;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes the 26.2 overworld sky disc an unbroken white field in the Light Realm. */
@Mixin(SkyRenderer.class)
abstract class LightRealmSkyMixin {
	@Unique
	private LightRealmSkyClientState powers$sky;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void powers$createLightRealmSky(TextureManager textures, AtlasManager atlases,
			RenderTarget renderTarget, CallbackInfo info) {
		powers$sky = new LightRealmSkyClientState(renderTarget);
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void powers$extractWhiteLightRealmSky(ClientLevel level, float partialTick,
			Camera camera, SkyRenderState state, CallbackInfo info) {
		boolean lightRealm = level.dimension().identifier().equals(PowersMod.id("light_realm"));
		if (lightRealm) {
			state.skybox = DimensionType.Skybox.OVERWORLD;
			state.skyColor = 0xFFFFFFFF;
			state.sunriseAndSunsetColor = 0;
			state.rainBrightness = 0.0f;
			state.starBrightness = 0.0f;
			state.shouldRenderDarkDisc = false;
		}
		LightRealmSkyProfile profile = LightRealmSkyRules.resolve(lightRealm,
				FxAccessibility.reducedMotion(Minecraft.getInstance()), powers$sky != null
						&& powers$sky.enhancedAvailable(), level.getGameTime() + partialTick);
		if (powers$sky != null) powers$sky.update(profile);
	}

	@Inject(method = "renderSkyDisc", at = @At("TAIL"))
	private void powers$renderAncientWhiteSky(int color, CallbackInfo info) {
		if (powers$sky != null) powers$sky.tryRender();
	}

	@Inject(method = "close", at = @At("HEAD"))
	private void powers$closeLightRealmSky(CallbackInfo info) {
		if (powers$sky != null) powers$sky.close();
	}
}

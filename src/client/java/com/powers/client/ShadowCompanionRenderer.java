package com.powers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.powers.companion.ShadowCompanionEntity;
import com.powers.animation.CastingPoseAngles;
import com.powers.animation.CastingPoseLocomotion;
import com.powers.client.animation.CastingAvatarRenderState;
import com.powers.client.animation.CastingPlayerModel;
import com.powers.client.animation.ClientCastingPoseManager;
import com.powers.client.fx.FxAccessibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;

/** Renders the real revealed Shadow with its owner's skin and empty player model. */
public final class ShadowCompanionRenderer extends MobRenderer<ShadowCompanionEntity,
		CastingAvatarRenderState, CastingPlayerModel> {
	private final CastingPlayerModel wideModel;
	private final CastingPlayerModel slimModel;

	public ShadowCompanionRenderer(EntityRendererProvider.Context context) {
		super(context, new CastingPlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.45F);
		wideModel = model;
		slimModel = new CastingPlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
	}

	@Override
	public Identifier getTextureLocation(CastingAvatarRenderState state) {
		return state.skin.body().texturePath();
	}

	@Override
	public CastingAvatarRenderState createRenderState() {
		return new CastingAvatarRenderState();
	}

	@Override
	public void extractRenderState(ShadowCompanionEntity entity,
			CastingAvatarRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks,
				itemModelResolver);
		var connection = Minecraft.getInstance().getConnection();
		var profile = entity.ownerProfile();
		var ownerId = profile.id() == null ? entity.getUUID() : profile.id();
		PlayerInfo owner = connection == null ? null : connection.getPlayerInfo(ownerId);
		state.skin = owner == null ? DefaultPlayerSkin.get(ownerId) : owner.getSkin();
		state.showHat = true;
		state.showJacket = true;
		state.showLeftPants = true;
		state.showRightPants = true;
		state.showLeftSleeve = true;
		state.showRightSleeve = true;
		state.castingAngles = ClientCastingPoseManager.resolve(entity)
				.map(resolved -> CastingPoseAngles.resolve(resolved.event().pose(),
						resolved.event().style(), resolved.event().hand(), resolved.progress(),
						FxAccessibility.reducedMotion(Minecraft.getInstance())).scale(
								CastingPoseLocomotion.scale(state.isFallFlying,
										state.isVisuallySwimming, state.swimAmount,
										state.walkAnimationSpeed, state.isPassenger)))
				.orElse(CastingPoseAngles.ZERO);
	}

	@Override
	public void submit(CastingAvatarRenderState state, PoseStack poseStack,
			SubmitNodeCollector collector, CameraRenderState camera) {
		model = state.skin.model() == PlayerModelType.SLIM ? slimModel : wideModel;
		super.submit(state, poseStack, collector, camera);
	}
}

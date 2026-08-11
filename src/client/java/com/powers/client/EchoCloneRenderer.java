package com.powers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.powers.entity.EchoClone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerModelType;

/** Renders an Orange Crystal echo with the owner's resolved wide/slim skin. */
public final class EchoCloneRenderer extends MobRenderer<EchoClone, AvatarRenderState, PlayerModel> {
	private final PlayerModel wideModel;
	private final PlayerModel slimModel;

	public EchoCloneRenderer(EntityRendererProvider.Context context) {
		super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.45F);
		wideModel = model;
		slimModel = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
	}

	@Override
	public Identifier getTextureLocation(AvatarRenderState state) {
		return state.skin.body().texturePath();
	}

	@Override
	public AvatarRenderState createRenderState() {
		return new AvatarRenderState();
	}

	@Override
	public void extractRenderState(EchoClone entity, AvatarRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks,
				itemModelResolver);
		var connection = Minecraft.getInstance().getConnection();
		var ownerProfile = entity.ownerProfile();
		var ownerId = ownerProfile.id() == null ? entity.getUUID() : ownerProfile.id();
		PlayerInfo owner = connection == null ? null
				: connection.getPlayerInfo(ownerId);
		state.skin = owner == null ? DefaultPlayerSkin.get(ownerId) : owner.getSkin();
		state.showHat = true;
		state.showJacket = true;
		state.showLeftPants = true;
		state.showRightPants = true;
		state.showLeftSleeve = true;
		state.showRightSleeve = true;
	}

	@Override
	public void submit(AvatarRenderState state, PoseStack poseStack,
			SubmitNodeCollector collector, CameraRenderState camera) {
		model = state.skin.model() == PlayerModelType.SLIM ? slimModel : wideModel;
		super.submit(state, poseStack, collector, camera);
	}
}

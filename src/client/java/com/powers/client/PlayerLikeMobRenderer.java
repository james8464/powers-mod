package com.powers.client;

import com.powers.PowersMod;
import com.powers.animation.CastingPoseAngles;
import com.powers.client.animation.CastingHumanoidModel;
import com.powers.client.animation.CastingHumanoidRenderState;
import com.powers.client.animation.ClientCastingPoseManager;
import com.powers.client.fx.FxAccessibility;
import com.powers.entity.AbstractPlayerLikeMob;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.Identifier;

/** Uses vanilla's full player proportions with a mod-owned 64x64 skin. */
public final class PlayerLikeMobRenderer extends MobRenderer<AbstractPlayerLikeMob,
		CastingHumanoidRenderState, CastingHumanoidModel> {
	private final Identifier texture;

	public PlayerLikeMobRenderer(EntityRendererProvider.Context context, String texture) {
		this(context, texture, 0.5F);
	}

	/** Allows private apparitions to omit the solid-world mob shadow. */
	public PlayerLikeMobRenderer(EntityRendererProvider.Context context, String texture, float shadowRadius) {
		super(context, new CastingHumanoidModel(context.bakeLayer(ModelLayers.PLAYER)), shadowRadius);
		this.texture = PowersMod.id("textures/entity/" + texture + ".png");
		addLayer(new ItemInHandLayer<>(this));
	}

	@Override
	public Identifier getTextureLocation(CastingHumanoidRenderState state) {
		return texture;
	}

	@Override
	public CastingHumanoidRenderState createRenderState() {
		return new CastingHumanoidRenderState();
	}

	@Override
	public void extractRenderState(AbstractPlayerLikeMob entity,
			CastingHumanoidRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, itemModelResolver);
		state.castingAngles = ClientCastingPoseManager.resolve(entity)
				.map(resolved -> CastingPoseAngles.resolve(resolved.event().pose(),
						resolved.event().style(), resolved.event().hand(), resolved.progress(),
						FxAccessibility.reducedMotion(Minecraft.getInstance())))
				.orElse(CastingPoseAngles.ZERO);
	}
}

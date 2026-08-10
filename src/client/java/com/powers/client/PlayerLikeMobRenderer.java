package com.powers.client;

import com.powers.PowersMod;
import com.powers.entity.AbstractPlayerLikeMob;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Uses vanilla's full player proportions with a mod-owned 64x64 skin. */
public final class PlayerLikeMobRenderer extends MobRenderer<AbstractPlayerLikeMob,
		HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	private final Identifier texture;

	public PlayerLikeMobRenderer(EntityRendererProvider.Context context, String texture) {
		this(context, texture, 0.5F);
	}

	/** Allows private apparitions to omit the solid-world mob shadow. */
	public PlayerLikeMobRenderer(EntityRendererProvider.Context context, String texture, float shadowRadius) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), shadowRadius);
		this.texture = PowersMod.id("textures/entity/" + texture + ".png");
		addLayer(new ItemInHandLayer<>(this));
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return texture;
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public void extractRenderState(AbstractPlayerLikeMob entity,
			HumanoidRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, itemModelResolver);
	}
}

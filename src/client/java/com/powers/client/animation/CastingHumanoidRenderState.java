package com.powers.client.animation;

import com.powers.animation.CastingPoseAngles;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/** Humanoid state extension used only by scoped POWERS player-like entities. */
public final class CastingHumanoidRenderState extends HumanoidRenderState {
	public CastingPoseAngles castingAngles = CastingPoseAngles.ZERO;
}

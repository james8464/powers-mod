package com.powers.client.animation;

import com.powers.animation.CastingPoseAngles;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Player-avatar state extension used only by the revealed Shadow renderer. */
public final class CastingAvatarRenderState extends AvatarRenderState {
	public CastingPoseAngles castingAngles = CastingPoseAngles.ZERO;
}

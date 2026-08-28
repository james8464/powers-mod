package com.powers.client.animation;

import com.powers.animation.CastingPoseAngles;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Vanilla player animation followed by bounded Shadow-only casting deltas. */
public final class CastingPlayerModel extends PlayerModel {
	public CastingPlayerModel(ModelPart root, boolean slim) {
		super(root, slim);
	}

	@Override
	public void setupAnim(AvatarRenderState state) {
		super.setupAnim(state);
		if (state instanceof CastingAvatarRenderState casting) apply(casting.castingAngles);
	}

	private void apply(CastingPoseAngles angles) {
		head.xRot += (float) angles.headX();
		head.yRot += (float) angles.headY();
		body.xRot += (float) angles.bodyX();
		body.yRot += (float) angles.bodyY();
		leftArm.xRot += (float) angles.leftArmX();
		leftArm.yRot += (float) angles.leftArmY();
		leftArm.zRot += (float) angles.leftArmZ();
		rightArm.xRot += (float) angles.rightArmX();
		rightArm.yRot += (float) angles.rightArmY();
		rightArm.zRot += (float) angles.rightArmZ();
	}
}

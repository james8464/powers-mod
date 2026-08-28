package com.powers.client.animation;

import com.powers.animation.CastingPoseAngles;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

/** Vanilla humanoid animation followed by bounded VFX-006 joint deltas. */
public final class CastingHumanoidModel extends HumanoidModel<CastingHumanoidRenderState> {
	public CastingHumanoidModel(ModelPart root) {
		super(root);
	}

	@Override
	public void setupAnim(CastingHumanoidRenderState state) {
		super.setupAnim(state);
		apply(state.castingAngles);
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

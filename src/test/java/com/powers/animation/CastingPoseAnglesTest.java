package com.powers.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CastingPoseAnglesTest {
	@Test
	void everyPoseAndStyleStaysInsideJointCaps() {
		for (CastingPose pose : CastingPose.values()) {
			for (CastingStyle style : CastingStyle.values()) {
				for (CastingHand hand : CastingHand.values()) {
					for (double progress : new double[]{0, 0.1, 0.2, 0.5, 0.8, 0.99, 1}) {
						var angles = CastingPoseAngles.resolve(pose, style, hand, progress, false);
						assertTrue(maxArm(angles) <= 1.25 + 1.0E-9);
						assertTrue(Math.abs(angles.bodyX()) <= 0.35 + 1.0E-9);
						assertTrue(Math.abs(angles.bodyY()) <= 0.35 + 1.0E-9);
						assertTrue(Math.abs(angles.headX()) <= 0.25 + 1.0E-9);
						assertTrue(Math.abs(angles.headY()) <= 0.25 + 1.0E-9);
					}
				}
			}
		}
	}

	@Test
	void handedProjectPoseMirrorsTheDominantArm() {
		var right = CastingPoseAngles.resolve(CastingPose.PROJECT, CastingStyle.RADIANT,
				CastingHand.RIGHT, 0.5, false);
		var left = CastingPoseAngles.resolve(CastingPose.PROJECT, CastingStyle.RADIANT,
				CastingHand.LEFT, 0.5, false);
		assertTrue(Math.abs(right.rightArmX()) > Math.abs(right.leftArmX()));
		assertEquals(right.rightArmX(), left.leftArmX());
		assertEquals(right.leftArmX(), left.rightArmX());
	}

	@Test
	void normalMotionEasesInHoldsAndEasesOut() {
		var zero = CastingPoseAngles.resolve(CastingPose.INVOKE, CastingStyle.SHADOW,
				CastingHand.BOTH, 0, false);
		var early = CastingPoseAngles.resolve(CastingPose.INVOKE, CastingStyle.SHADOW,
				CastingHand.BOTH, 0.1, false);
		var hold = CastingPoseAngles.resolve(CastingPose.INVOKE, CastingStyle.SHADOW,
				CastingHand.BOTH, 0.5, false);
		var late = CastingPoseAngles.resolve(CastingPose.INVOKE, CastingStyle.SHADOW,
				CastingHand.BOTH, 0.9, false);
		assertEquals(CastingPoseAngles.ZERO, zero);
		assertTrue(maxArm(early) < maxArm(hold));
		assertTrue(maxArm(late) < maxArm(hold));
	}

	@Test
	void reducedMotionIsStaticDirectionalAndLowerAmplitude() {
		var early = CastingPoseAngles.resolve(CastingPose.PROJECT, CastingStyle.RADIANT,
				CastingHand.RIGHT, 0.2, true);
		var late = CastingPoseAngles.resolve(CastingPose.PROJECT, CastingStyle.RADIANT,
				CastingHand.RIGHT, 0.8, true);
		var normal = CastingPoseAngles.resolve(CastingPose.PROJECT, CastingStyle.RADIANT,
				CastingHand.RIGHT, 0.5, false);
		assertEquals(early, late);
		assertNotEquals(CastingPoseAngles.ZERO, early);
		assertTrue(maxArm(early) < maxArm(normal));
		assertTrue(Math.abs(early.rightArmX()) > Math.abs(early.leftArmX()));
	}

	private static double maxArm(CastingPoseAngles value) {
		return Math.max(Math.max(Math.abs(value.leftArmX()), Math.abs(value.leftArmY())),
				Math.max(Math.max(Math.abs(value.leftArmZ()), Math.abs(value.rightArmX())),
						Math.max(Math.abs(value.rightArmY()), Math.abs(value.rightArmZ()))));
	}
}

package com.powers.animation;

import java.util.Objects;

/** Pure bounded joint deltas applied after vanilla humanoid animation. */
public record CastingPoseAngles(double headX, double headY, double bodyX, double bodyY,
		double leftArmX, double leftArmY, double leftArmZ,
		double rightArmX, double rightArmY, double rightArmZ) {
	public static final CastingPoseAngles ZERO = new CastingPoseAngles(
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

	public static CastingPoseAngles resolve(CastingPose pose, CastingStyle style, CastingHand hand,
			double progress, boolean reducedMotion) {
		Objects.requireNonNull(pose, "pose");
		Objects.requireNonNull(style, "style");
		Objects.requireNonNull(hand, "hand");
		if (!Double.isFinite(progress)) throw new IllegalArgumentException("progress");
		double amplitude = reducedMotion ? 0.55 : amplitude(Math.clamp(progress, 0.0, 1.0));
		if (amplitude == 0) return ZERO;
		Base base = switch (pose) {
			case INVOKE -> new Base(-0.05, -0.08, -0.84, -0.84, 0.22, -0.22, 0.16, -0.16);
			case PROJECT -> new Base(-0.02, -0.04, -0.34, -1.18, 0.05, -0.16, 0.05, -0.08);
			case CHANNEL -> new Base(0.08, -0.12, -0.98, -0.98, 0.18, -0.18, 0.08, -0.08);
			case RELEASE -> new Base(-0.10, -0.18, -1.10, -1.10, 0.28, -0.28, 0.22, -0.22);
		};
		double leftX = base.leftX;
		double rightX = base.rightX;
		double leftY = base.leftY;
		double rightY = base.rightY;
		double leftZ = base.leftZ;
		double rightZ = base.rightZ;
		if (pose == CastingPose.PROJECT && hand == CastingHand.LEFT) {
			double swapX = leftX;
			leftX = rightX;
			rightX = swapX;
			double swapY = leftY;
			leftY = -rightY;
			rightY = -swapY;
			double swapZ = leftZ;
			leftZ = -rightZ;
			rightZ = -swapZ;
		} else if (pose == CastingPose.PROJECT && (hand == CastingHand.BOTH || hand == CastingHand.NONE)) {
			leftX = rightX = hand == CastingHand.BOTH ? -1.08 : -0.72;
			leftY = 0.14;
			rightY = -0.14;
			leftZ = 0.06;
			rightZ = -0.06;
		}
		double styleScale = switch (style) {
			case SHADOW, DARKNESS, HERALD_DARK -> 1.0;
			case RADIANT, HERALD_LIGHT -> 0.94;
			case FIRST_VESSEL -> 1.04;
		};
		double bodyYaw = switch (style) {
			case SHADOW, DARKNESS, HERALD_DARK -> -0.08;
			case RADIANT, HERALD_LIGHT -> 0.08;
			case FIRST_VESSEL -> 0.0;
		};
		double factor = amplitude * styleScale;
		return new CastingPoseAngles(
				clamp(base.headX * factor, 0.25), clamp(bodyYaw * factor * 0.45, 0.25),
				clamp(base.bodyX * factor, 0.35), clamp(bodyYaw * factor, 0.35),
				clamp(leftX * factor, 1.25), clamp(leftY * factor, 1.25),
				clamp(leftZ * factor, 1.25), clamp(rightX * factor, 1.25),
				clamp(rightY * factor, 1.25), clamp(rightZ * factor, 1.25));
	}

	public CastingPoseAngles scale(double factor) {
		if (!Double.isFinite(factor) || factor < 0.0 || factor > 1.0) {
			throw new IllegalArgumentException("factor");
		}
		if (factor == 0.0) return ZERO;
		if (factor == 1.0) return this;
		return new CastingPoseAngles(headX * factor, headY * factor, bodyX * factor,
				bodyY * factor, leftArmX * factor, leftArmY * factor, leftArmZ * factor,
				rightArmX * factor, rightArmY * factor, rightArmZ * factor);
	}

	private static double amplitude(double progress) {
		if (progress <= 0 || progress >= 1) return 0;
		if (progress < 0.2) return smoothstep(progress / 0.2);
		if (progress <= 0.75) return 1;
		return smoothstep((1 - progress) / 0.25);
	}

	private static double smoothstep(double value) {
		double clamped = Math.clamp(value, 0.0, 1.0);
		return clamped * clamped * (3 - 2 * clamped);
	}

	private static double clamp(double value, double limit) {
		return Math.clamp(value, -limit, limit);
	}

	private record Base(double headX, double bodyX, double leftX, double rightX,
			double leftY, double rightY, double leftZ, double rightZ) {
	}
}

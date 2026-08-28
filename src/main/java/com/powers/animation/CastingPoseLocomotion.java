package com.powers.animation;

/** Pure compatibility scale that lets vanilla locomotion retain priority over casting deltas. */
public final class CastingPoseLocomotion {
	private CastingPoseLocomotion() {
	}

	public static double scale(boolean fallFlying, boolean visuallySwimming, float swimAmount,
			float walkAnimationSpeed, boolean passenger) {
		if (!Float.isFinite(swimAmount) || !Float.isFinite(walkAnimationSpeed)) return 0.0;
		if (fallFlying || visuallySwimming || swimAmount > 0.2F) return 0.0;
		if (passenger) return 0.5;
		double speed = Math.max(0.0, walkAnimationSpeed);
		if (speed <= 0.25) return 1.0;
		return Math.clamp(1.0 - (speed - 0.25) * 0.6, 0.55, 1.0);
	}
}

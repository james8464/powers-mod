package com.powers.visual;

/** Pure client-camera policy for Chronal Overdrive's optional FOV presentation. */
public final class SuperSpeedCameraRules {
	private static final float MAX_FOV_EXPANSION = 12.0F;

	private SuperSpeedCameraRules() {
	}

	/** Clamps speed expansion to the vanilla slider and removes it under reduced motion. */
	public static float resolveFov(float vanillaFov, float configuredFov,
			boolean active, boolean reducedMotion, double effectScale) {
		if (!Float.isFinite(configuredFov)) configuredFov = 70.0F;
		configuredFov = Math.clamp(configuredFov, 30.0F, 110.0F);
		if (!active) return vanillaFov;
		if (reducedMotion || !Float.isFinite(vanillaFov)) return configuredFov;
		double scale = Double.isFinite(effectScale) ? Math.clamp(effectScale, 0.0, 1.0) : 0.0;
		return Math.min(vanillaFov, configuredFov + MAX_FOV_EXPANSION * (float) scale);
	}
}

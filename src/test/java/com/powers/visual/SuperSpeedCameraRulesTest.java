package com.powers.visual;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SuperSpeedCameraRulesTest {
	@Test
	void inactiveCameraPreservesVanillaFovExactly() {
		assertEquals(96.0F, SuperSpeedCameraRules.resolveFov(
				96.0F, 70.0F, false, false, 1.0), 0.0001F);
	}

	@Test
	void activeCameraClampsExpansionAndHonorsVanillaEffectScale() {
		assertEquals(82.0F, SuperSpeedCameraRules.resolveFov(
				96.0F, 70.0F, true, false, 1.0), 0.0001F);
		assertEquals(76.0F, SuperSpeedCameraRules.resolveFov(
				96.0F, 70.0F, true, false, 0.5), 0.0001F);
		assertEquals(70.0F, SuperSpeedCameraRules.resolveFov(
				96.0F, 70.0F, true, false, 0.0), 0.0001F);
	}

	@Test
	void reducedMotionRemovesOverdriveFovDistortionWithoutChangingGameplay() {
		assertEquals(70.0F, SuperSpeedCameraRules.resolveFov(
				96.0F, 70.0F, true, true, 1.0), 0.0001F);
		assertEquals(70.0F, SuperSpeedCameraRules.resolveFov(
				Float.NaN, 70.0F, true, false, 1.0), 0.0001F);
	}
}

package com.powers.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SoulLinkMathTest {
	@Test
	void topologyNeverExceedsEightAndMirroredBaselineExcludesPriorReflection() {
		assertEquals(8, SoulLinkMath.maximumLinks());
		assertEquals(3.0F, SoulLinkMath.woundAfterMirror(20.0F, 15.0F, 12.0F));
		assertEquals(8.0F, SoulLinkMath.woundAfterMirror(20.0F, null, 12.0F));
	}
	@Test
	void findsLargestRealWoundAndSnapshotsPostMirrorHealth() {
		assertEquals(4.0f, SoulLinkMath.largestWound(
				new float[] {20.0f, 18.0f, 12.0f}, new float[] {19.0f, 14.0f, 12.0f}));
		assertArrayEquals(new float[] {19.0f, 10.0f, 8.0f},
				SoulLinkMath.snapshot(new float[] {19.0f, 10.0f, 8.0f}));
	}

	@Test void mirroredDamageIsCappedPerTargetAcrossTheWholeLink() {
		assertEquals(6.0f, SoulLinkMath.cappedMirror(6.0f, 10.0f));
		assertEquals(4.0f, SoulLinkMath.cappedMirror(6.0f, 4.0f));
		assertEquals(0.0f, SoulLinkMath.remainingCap(4.0f, 6.0f));
		assertEquals(7.0f, SoulLinkMath.remainingCap(10.0f, 3.0f));
	}
}

package com.powers.power;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SoulLinkMathTest {
	@Test
	void findsLargestRealWoundAndSnapshotsPostMirrorHealth() {
		assertEquals(4.0f, SoulLinkMath.largestWound(
				new float[] {20.0f, 18.0f, 12.0f}, new float[] {19.0f, 14.0f, 12.0f}));
		assertArrayEquals(new float[] {19.0f, 10.0f, 8.0f},
				SoulLinkMath.snapshot(new float[] {19.0f, 10.0f, 8.0f}));
	}
}

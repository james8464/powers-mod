package com.powers.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShapeFxKindTest {
	@Test
	void accountsForTheActualParticlesInEachSemanticShape() {
		assertEquals(20, ShapeFxKind.RING.requestedParticles(20));
		assertEquals(50, ShapeFxKind.RUNE.requestedParticles(20));
		assertEquals(20, ShapeFxKind.SPIRAL.requestedParticles(20));
	}

	@Test
	void clampsInvalidOrExtremeAuthoredPointCounts() {
		assertEquals(0, ShapeFxKind.RING.requestedParticles(-1));
		assertEquals(256, ShapeFxKind.RING.requestedParticles(1_000));
		assertEquals(640, ShapeFxKind.RUNE.requestedParticles(1_000));
	}
}

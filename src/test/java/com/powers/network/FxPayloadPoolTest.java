package com.powers.network;

import com.powers.fx.BeamFxStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FxPayloadPoolTest {
	@Test
	void identicalImmutablePayloadBuildersReuseOneBoundedInstance() {
		FxPayloadPool pool = new FxPayloadPool(2);
		var first = pool.intern(new MagicFxPackets.BeamFxPayload(
				1, BeamFxStyle.COLORED, 0, 0, 0, 1, 1, 1, 8, 0xFFFFFF));
		var second = pool.intern(new MagicFxPackets.BeamFxPayload(
				1, BeamFxStyle.COLORED, 0, 0, 0, 1, 1, 1, 8, 0xFFFFFF));
		assertTrue(first == second);
		pool.intern(new MagicFxPackets.BeamFxPayload(
				2, BeamFxStyle.COLORED, 0, 0, 0, 1, 1, 1, 8, 0xFFFFFF));
		pool.intern(new MagicFxPackets.BeamFxPayload(
				3, BeamFxStyle.COLORED, 0, 0, 0, 1, 1, 1, 8, 0xFFFFFF));
		assertTrue(pool.size() <= 2);
	}
}

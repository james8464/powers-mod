package com.powers.testing;

import com.powers.network.FxPacketCoalescer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxCoalescingCaptureTest {
	@Test
	void acceptsOnlyRealReductionsMeetingBothPerf005Gates() {
		var accepted = FxCoalescingCapture.evaluate(
				new FxPacketCoalescer.TrafficSnapshot(64, 1, 3_520, 55));
		var packetOnly = FxCoalescingCapture.evaluate(
				new FxPacketCoalescer.TrafficSnapshot(4, 3, 400, 301));

		assertTrue(accepted.passed());
		assertTrue(accepted.marker().contains("attemptedPackets=64"));
		assertTrue(accepted.marker().contains("deliveredBytes=55"));
		assertFalse(packetOnly.passed());
	}
}

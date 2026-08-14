package com.powers.network;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxPacketCoalescerTest {
	@Test
	void repeatedActionChunkObserverUpdatesCoalesceForOneTickOnly() {
		FxPacketCoalescer coalescer = new FxPacketCoalescer(8);
		UUID observer = new UUID(0L, 1L);
		assertTrue(coalescer.allow(10, observer, "overworld", 2, 3,
				"energy_beam", "sustain", 64));
		assertFalse(coalescer.allow(10, observer, "overworld", 2, 3,
				"energy_beam", "sustain", 64));
		assertTrue(coalescer.allow(10, observer, "overworld", 2, 3,
				"lightning", "sustain", 72));
		assertTrue(coalescer.allow(10, new UUID(0L, 2L), "overworld", 2, 3,
				"energy_beam", "sustain", 64));
		assertTrue(coalescer.allow(11, observer, "overworld", 2, 3,
				"energy_beam", "sustain", 64));
	}

	@Test
	void semanticPhasesRemainVisuallyDistinctAndTrafficIsMeasured() {
		FxPacketCoalescer coalescer = new FxPacketCoalescer(8);
		UUID observer = new UUID(0L, 1L);

		assertTrue(coalescer.allow(20, observer, "overworld", 4, 5,
				"energy_beam", "sustain", 64));
		assertFalse(coalescer.allow(20, observer, "overworld", 4, 5,
				"energy_beam", "sustain", 64));
		assertTrue(coalescer.allow(20, observer, "overworld", 4, 5,
				"energy_beam", "impact", 80));

		assertEquals(new FxPacketCoalescer.TrafficSnapshot(3, 2, 208, 144),
				coalescer.trafficSnapshot());
		assertEquals(33.333, coalescer.trafficSnapshot().packetReductionPercent(), 0.001);
		assertEquals(30.769, coalescer.trafficSnapshot().byteReductionPercent(), 0.001);

		coalescer.clear();
		assertEquals(new FxPacketCoalescer.TrafficSnapshot(0, 0, 0, 0),
				coalescer.trafficSnapshot());
	}

	@Test
	void boundedCapacityFailsOpenInsteadOfHidingNewMeaning() {
		FxPacketCoalescer coalescer = new FxPacketCoalescer(1);
		UUID observer = new UUID(0L, 1L);
		assertTrue(coalescer.allow(10, observer, "overworld", 0, 0,
				"one", "sustain", 32));
		assertTrue(coalescer.allow(10, observer, "overworld", 1, 0,
				"two", "sustain", 48));
	}
}

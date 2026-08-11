package com.powers.network;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxPacketCoalescerTest {
	@Test
	void repeatedActionChunkObserverUpdatesCoalesceForOneTickOnly() {
		FxPacketCoalescer coalescer = new FxPacketCoalescer(8);
		UUID observer = new UUID(0L, 1L);
		assertTrue(coalescer.allow(10, observer, "overworld", 2, 3, "beam:colored"));
		assertFalse(coalescer.allow(10, observer, "overworld", 2, 3, "beam:colored"));
		assertTrue(coalescer.allow(10, observer, "overworld", 2, 3, "beam:electric"));
		assertTrue(coalescer.allow(10, new UUID(0L, 2L), "overworld", 2, 3, "beam:colored"));
		assertTrue(coalescer.allow(11, observer, "overworld", 2, 3, "beam:colored"));
	}

	@Test
	void boundedCapacityFailsOpenInsteadOfHidingNewMeaning() {
		FxPacketCoalescer coalescer = new FxPacketCoalescer(1);
		UUID observer = new UUID(0L, 1L);
		assertTrue(coalescer.allow(10, observer, "overworld", 0, 0, "one"));
		assertTrue(coalescer.allow(10, observer, "overworld", 1, 0, "two"));
	}
}

package com.powers.power.travel;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeleportStormTrackerTest {
	@Test
	void oneOwnerCannotStartTwoStormsAndCleanupAllowsTheNext() {
		TeleportStormTracker tracker = new TeleportStormTracker();
		UUID owner = UUID.randomUUID();
		assertTrue(tracker.begin(owner));
		assertTrue(tracker.active(owner));
		assertFalse(tracker.begin(owner));
		assertTrue(tracker.finish(owner));
		assertFalse(tracker.active(owner));
		assertTrue(tracker.begin(owner));
	}

	@Test
	void nullAndUnknownOwnersCannotCorruptTheTracker() {
		TeleportStormTracker tracker = new TeleportStormTracker();
		assertFalse(tracker.begin(null));
		assertFalse(tracker.active(null));
		assertFalse(tracker.finish(UUID.randomUUID()));
		assertTrue(tracker.activeCount() == 0);
	}
}

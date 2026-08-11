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
		assertFalse(tracker.begin(owner));
		assertTrue(tracker.finish(owner));
		assertTrue(tracker.begin(owner));
	}

	@Test
	void nullAndUnknownOwnersCannotCorruptTheTracker() {
		TeleportStormTracker tracker = new TeleportStormTracker();
		assertFalse(tracker.begin(null));
		assertFalse(tracker.finish(UUID.randomUUID()));
		assertTrue(tracker.activeCount() == 0);
	}
}

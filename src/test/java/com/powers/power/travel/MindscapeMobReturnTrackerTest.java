package com.powers.power.travel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MindscapeMobReturnTrackerTest {
	@Test
	void trackerIsBoundedAndOnlyOwnsOrdinaryLivingMobs() {
		assertEquals(256, MindscapeMobReturnTracker.MAX_TRACKED);
		assertEquals(MindscapeMobReturnTracker.TrackDecision.TRACK,
				MindscapeMobReturnTracker.trackDecision(true, false, false));
		assertEquals(MindscapeMobReturnTracker.TrackDecision.SKIP_PLAYER,
				MindscapeMobReturnTracker.trackDecision(true, true, false));
		assertEquals(MindscapeMobReturnTracker.TrackDecision.SKIP_SHADOW,
				MindscapeMobReturnTracker.trackDecision(true, false, true));
		assertEquals(MindscapeMobReturnTracker.TrackDecision.SKIP_DEAD,
				MindscapeMobReturnTracker.trackDecision(false, false, false));
	}
}

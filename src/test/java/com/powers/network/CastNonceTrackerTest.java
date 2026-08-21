package com.powers.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CastNonceTrackerTest {
	@Test
	void nonceCanBeConsumedOnlyOnceByItsOwnerBeforeExpiry() {
		CastNonceTracker tracker = new CastNonceTracker(100);
		UUID owner = UUID.randomUUID();
		UUID stranger = UUID.randomUUID();
		UUID nonce = tracker.issue(owner, 20);
		assertTrue(tracker.contains(owner));

		assertFalse(tracker.consume(stranger, nonce, 21));
		assertTrue(tracker.consume(owner, nonce, 21));
		assertFalse(tracker.contains(owner));
		assertFalse(tracker.consume(owner, nonce, 22));
	}

	@Test
	void expiredNonceIsRejected() {
		CastNonceTracker tracker = new CastNonceTracker(20);
		UUID owner = UUID.randomUUID();
		UUID nonce = tracker.issue(owner, 5);
		assertFalse(tracker.consume(owner, nonce, 26));
		assertEquals(0, tracker.size());
	}

	@Test
	void lifecycleCleanupForgetsEveryOutstandingNonce() {
		CastNonceTracker tracker = new CastNonceTracker(100);
		tracker.issue(UUID.randomUUID(), 5);
		tracker.issue(UUID.randomUUID(), 5);
		assertEquals(2, tracker.size());

		tracker.clearAll();

		assertEquals(0, tracker.size());
	}
}

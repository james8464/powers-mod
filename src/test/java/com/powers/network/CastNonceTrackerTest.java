package com.powers.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

		assertFalse(tracker.consume(stranger, nonce, 21));
		assertTrue(tracker.consume(owner, nonce, 21));
		assertFalse(tracker.consume(owner, nonce, 22));
	}

	@Test
	void expiredNonceIsRejected() {
		CastNonceTracker tracker = new CastNonceTracker(20);
		UUID owner = UUID.randomUUID();
		UUID nonce = tracker.issue(owner, 5);
		assertFalse(tracker.consume(owner, nonce, 26));
	}
}

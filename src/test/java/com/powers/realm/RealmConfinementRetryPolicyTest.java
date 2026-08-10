package com.powers.realm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmConfinementRetryPolicyTest {
	@Test
	void retriesAreFiniteAndUseCappedExponentialBackoff() {
		assertTrue(RealmConfinementRetryPolicy.shouldRetry(0));
		assertTrue(RealmConfinementRetryPolicy.shouldRetry(4));
		assertFalse(RealmConfinementRetryPolicy.shouldRetry(5));
		assertEquals(100, RealmConfinementRetryPolicy.delayTicks(0));
		assertEquals(800, RealmConfinementRetryPolicy.delayTicks(3));
		assertEquals(1600, RealmConfinementRetryPolicy.delayTicks(20));
	}
}

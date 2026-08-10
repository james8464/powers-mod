package com.powers.forge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrucibleCastRateLimiterTest {
	@Test
	void permitsAtMostOneWeaponCastPerPlayerPerTick() {
		CrucibleCastRateLimiter limiter = new CrucibleCastRateLimiter();
		UUID first = UUID.randomUUID();
		UUID second = UUID.randomUUID();
		assertTrue(limiter.allow(first, 10));
		assertFalse(limiter.allow(first, 10));
		assertTrue(limiter.allow(first, 11));
		assertTrue(limiter.allow(second, 10));
	}
}

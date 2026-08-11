package com.powers.protection;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsentAuditRateLimiterTest {
	@Test
	void repeatedDenialsAreSuppressedUntilTheBoundedWindowExpires() {
		ConsentAuditRateLimiter limiter = new ConsentAuditRateLimiter(2, 100);
		UUID caster = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
		UUID target = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

		assertTrue(limiter.shouldLog(20, caster, target, ConsentKind.TELEPORT,
				ConsentOverrideRules.Decision.DENY_SAFE_ZONE));
		assertFalse(limiter.shouldLog(119, caster, target, ConsentKind.TELEPORT,
				ConsentOverrideRules.Decision.DENY_SAFE_ZONE));
		assertTrue(limiter.shouldLog(120, caster, target, ConsentKind.TELEPORT,
				ConsentOverrideRules.Decision.DENY_SAFE_ZONE));
	}

	@Test
	void limiterEvictsOldestKeysAndNeverExceedsItsFixedCapacity() {
		ConsentAuditRateLimiter limiter = new ConsentAuditRateLimiter(2, 100);
		UUID target = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
		UUID first = UUID.fromString("123e4567-e89b-12d3-a456-426614174010");
		UUID second = UUID.fromString("123e4567-e89b-12d3-a456-426614174011");
		UUID third = UUID.fromString("123e4567-e89b-12d3-a456-426614174012");
		assertTrue(limiter.shouldLog(1, first, target, ConsentKind.TELEPORT,
				ConsentOverrideRules.Decision.DENY_CONSENT));
		assertTrue(limiter.shouldLog(1, second, target, ConsentKind.TELEPORT,
				ConsentOverrideRules.Decision.DENY_CONSENT));
		assertTrue(limiter.shouldLog(1, third, target, ConsentKind.TELEPORT,
				ConsentOverrideRules.Decision.DENY_CONSENT));
		assertTrue(limiter.shouldLog(2, first, target, ConsentKind.TELEPORT,
				ConsentOverrideRules.Decision.DENY_CONSENT));
		assertTrue(limiter.size() <= 2);
	}
}

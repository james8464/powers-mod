package com.powers.network;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketRateLimiterTest {
	private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

	@Test
	void eachLaneHasAnIndependentHardPerSecondBudget() {
		PacketRateLimiter limiter = new PacketRateLimiter();
		for (int request = 0; request < PacketRateLimiter.Lane.TRAVEL.limit(); request++) {
			assertTrue(limiter.allow(PLAYER, PacketRateLimiter.Lane.TRAVEL, 100));
		}
		assertFalse(limiter.allow(PLAYER, PacketRateLimiter.Lane.TRAVEL, 100));
		assertTrue(limiter.allow(PLAYER, PacketRateLimiter.Lane.ACTIVATION, 100));
	}

	@Test
	void aNewWindowClockRollbackAndDisconnectAllReleaseTheBudget() {
		PacketRateLimiter limiter = new PacketRateLimiter();
		for (int request = 0; request < PacketRateLimiter.Lane.LOCATOR.limit(); request++) {
			assertTrue(limiter.allow(PLAYER, PacketRateLimiter.Lane.LOCATOR, 100));
		}
		assertFalse(limiter.allow(PLAYER, PacketRateLimiter.Lane.LOCATOR, 119));
		assertTrue(limiter.allow(PLAYER, PacketRateLimiter.Lane.LOCATOR, 120));
		assertTrue(limiter.allow(PLAYER, PacketRateLimiter.Lane.LOCATOR, 1));
		limiter.forget(PLAYER);
		assertTrue(limiter.allow(PLAYER, PacketRateLimiter.Lane.LOCATOR, 1));
	}
}

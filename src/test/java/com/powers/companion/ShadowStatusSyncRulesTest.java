package com.powers.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowStatusSyncRulesTest {
	@Test
	void sendsOnSuppressionChangeLifecycleBoundaryOrBoundedHeartbeatOnly() {
		ShadowStatusSyncRules.Snapshot ordinary = snapshot(false);
		ShadowStatusSyncRules.Snapshot suppressed = snapshot(true);
		assertTrue(ShadowStatusSyncRules.shouldSend(ordinary, suppressed, 1, false));
		assertTrue(ShadowStatusSyncRules.shouldSend(ordinary, ordinary, 1, true));
		assertFalse(ShadowStatusSyncRules.shouldSend(ordinary, ordinary, 19, false));
		assertTrue(ShadowStatusSyncRules.shouldSend(ordinary, ordinary, 20, false));
	}

	private static ShadowStatusSyncRules.Snapshot snapshot(boolean suppressed) {
		return new ShadowStatusSyncRules.Snapshot(
				true, 900, "follow", false, suppressed, 0);
	}
}

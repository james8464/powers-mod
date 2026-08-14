package com.powers.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards conservative client-culling bounds for authored semantic geometry. */
class ServerFxTransportTest {
	@Test
	void spiralUsesItsCombinedRadialAndVerticalExtent() {
		assertTrue(ServerFxTransport.requiresDistanceOverride(11.0, 8.0, 20.0));
		assertFalse(ServerFxTransport.requiresDistanceOverride(8.0, 3.0, 4.0));
	}
}

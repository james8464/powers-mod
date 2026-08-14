package com.powers.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifecycleCallbackGateTest {
	@Test
	void staleCallbacksCannotCrossReloadOrShutdownEpochs() {
		LifecycleCallbackGate<String> gate = new LifecycleCallbackGate<>();
		long firstEpoch = gate.bind("first-server");

		assertEquals("first-server", gate.resolve(firstEpoch).orElseThrow());
		assertTrue(gate.clear("first-server"));
		assertTrue(gate.resolve(firstEpoch).isEmpty());

		long secondEpoch = gate.bind("second-server");
		assertTrue(gate.resolve(firstEpoch).isEmpty());
		assertEquals("second-server", gate.resolve(secondEpoch).orElseThrow());
	}
}

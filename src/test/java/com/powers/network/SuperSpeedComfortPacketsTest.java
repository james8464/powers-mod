package com.powers.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SuperSpeedComfortPacketsTest {
	@Test
	void lifecyclePayloadIsFiniteAndInternallyConsistent() {
		assertDoesNotThrow(() -> new SuperSpeedComfortPackets.Payload(true, 560));
		assertDoesNotThrow(() -> new SuperSpeedComfortPackets.Payload(false, 0));
		assertThrows(IllegalArgumentException.class,
				() -> new SuperSpeedComfortPackets.Payload(true, 0));
		assertThrows(IllegalArgumentException.class,
				() -> new SuperSpeedComfortPackets.Payload(false, 1));
		assertThrows(IllegalArgumentException.class,
				() -> new SuperSpeedComfortPackets.Payload(true, 2401));
	}
}

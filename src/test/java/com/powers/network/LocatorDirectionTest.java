package com.powers.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocatorDirectionTest {
	@Test
	void mapsWorldOffsetsToEightReadableDirections() {
		assertEquals("here", LocatorSpellPackets.compassDirection(0, 0));
		assertEquals("north", LocatorSpellPackets.compassDirection(0, -10));
		assertEquals("north-east", LocatorSpellPackets.compassDirection(10, -10));
		assertEquals("east", LocatorSpellPackets.compassDirection(10, 0));
		assertEquals("south-west", LocatorSpellPackets.compassDirection(-10, 10));
	}
}

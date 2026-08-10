package com.powers.mind;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BodyProxyTicketRulesTest {
	@Test
	void eachFrozenBodyKeepsOnlyItsOwnChunkSimulated() {
		assertEquals(0, BodyProxyTicketRules.radius());
		assertEquals(1, BodyProxyTicketRules.maximumChunksPerBody());
	}
}

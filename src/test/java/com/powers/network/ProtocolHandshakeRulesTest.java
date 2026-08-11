package com.powers.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolHandshakeRulesTest {
	@Test
	void acceptsTheExactProtocolAndReportsBothVersions() {
		var result = ProtocolHandshakeRules.validate(
				ProtocolHandshakeRules.CURRENT_PROTOCOL,
				ProtocolHandshakeRules.CURRENT_PROTOCOL,
				"1.0.2");

		assertTrue(result.accepted());
		assertTrue(result.message().contains("protocol " + ProtocolHandshakeRules.CURRENT_PROTOCOL));
		assertTrue(result.message().contains("mod 1.0.2"));
	}

	@Test
	void rejectsAProtocolMismatchWithAnActionableDisconnectReason() {
		var result = ProtocolHandshakeRules.validate(7, 4, "0.9.0");

		assertFalse(result.accepted());
		assertTrue(result.message().contains("server protocol 7"));
		assertTrue(result.message().contains("client protocol 4"));
		assertTrue(result.message().contains("client mod 0.9.0"));
		assertTrue(result.message().contains("same POWERS version"));
	}

	@Test
	void rejectsMalformedVersionsWithoutThrowing() {
		assertFalse(ProtocolHandshakeRules.validate(1, -1, "1.0.2").accepted());
		assertFalse(ProtocolHandshakeRules.validate(1, 1, " ").accepted());
		assertFalse(ProtocolHandshakeRules.validate(1, 1, "x".repeat(65)).accepted());
	}
}

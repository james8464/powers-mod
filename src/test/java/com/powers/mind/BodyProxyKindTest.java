package com.powers.mind;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BodyProxyKindTest {
	@Test
	void persistentNamesRoundTripAndUnknownValuesFailSafe() {
		for (BodyProxyKind kind : BodyProxyKind.values()) {
			assertEquals(kind, BodyProxyKind.fromSerialized(kind.serializedName()));
		}
		assertEquals(BodyProxyKind.REALM, BodyProxyKind.fromSerialized("unknown"));
	}
}

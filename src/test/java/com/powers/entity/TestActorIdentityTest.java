package com.powers.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestActorIdentityTest {
	private static final UUID FIRST = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
	private static final UUID SECOND = UUID.fromString("fedcba98-7654-3210-fedc-ba9876543210");

	@Test
	void defaultUsernameIsStableValidAndDistinct() {
		assertEquals("Test_01234567", TestActorIdentity.defaultUsername(FIRST));
		assertEquals(TestActorIdentity.defaultUsername(FIRST), TestActorIdentity.defaultUsername(FIRST));
		assertNotEquals(TestActorIdentity.defaultUsername(FIRST), TestActorIdentity.defaultUsername(SECOND));
		assertTrue(TestActorIdentity.defaultUsername(FIRST).matches("[A-Za-z0-9_]{1,16}"));
	}

	@Test
	void requestedUsernameIsTrimmedSanitizedAndBounded() {
		assertEquals("Test_Player_One", TestActorIdentity.normalize("  Test Player-One!  ", FIRST));
		assertEquals("abcdefghijklmnop", TestActorIdentity.normalize("abcdefghijklmnopQRST", FIRST));
	}

	@Test
	void emptyOrInvalidUsernameRecoversToDefault() {
		assertEquals("Test_01234567", TestActorIdentity.normalize("   ", FIRST));
		assertEquals("Test_01234567", TestActorIdentity.normalize("!!!", FIRST));
		assertEquals("Test_01234567", TestActorIdentity.normalize(null, FIRST));
	}
}

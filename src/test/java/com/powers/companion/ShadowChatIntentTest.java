package com.powers.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowChatIntentTest {
	@Test
	void parsesOnlyTheExplicitCaseInsensitivePrefix() {
		assertFalse(ShadowChatIntent.parse("hello shadow").addressed());
		assertFalse(ShadowChatIntent.parse("shadow reveal yourself").addressed());
		ShadowChatIntent intent = ShadowChatIntent.parse("  ShAdOw,   what is Pure Darkness? ");
		assertTrue(intent.addressed());
		assertEquals(ShadowChatIntent.Action.QUESTION, intent.action());
		assertEquals("what is Pure Darkness?", intent.message());
	}

	@Test
	void revealHideAndDismissAreDeterministicCommands() {
		assertEquals(ShadowChatIntent.Action.REVEAL,
				ShadowChatIntent.parse("shadow, reveal yourself").action());
		assertEquals(ShadowChatIntent.Action.HIDE,
				ShadowChatIntent.parse("shadow, hide yourself").action());
		assertEquals(ShadowChatIntent.Action.DISMISS,
				ShadowChatIntent.parse("shadow, leave me").action());
		assertEquals(ShadowChatIntent.Action.SUMMON,
				ShadowChatIntent.parse("shadow, come to me").action());
	}

	@Test
	void blankAndOversizedRequestsAreRejectedSafely() {
		assertEquals(ShadowChatIntent.Action.EMPTY, ShadowChatIntent.parse("shadow,  ").action());
		assertEquals(ShadowChatIntent.Action.TOO_LONG,
				ShadowChatIntent.parse("shadow, " + "x".repeat(257)).action());
	}
}

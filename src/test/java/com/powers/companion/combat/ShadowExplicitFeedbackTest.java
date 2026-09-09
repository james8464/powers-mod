package com.powers.companion.combat;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowExplicitFeedbackTest {
	@Test
	void explicitFeedbackIsSeparateCappedAndRequiresACompletedChoice() {
		ShadowLearningState state = new ShadowLearningState();
		assertFalse(state.applyExplicitFeedback(true));

		state.rememberChoice("far:boss", "boss", "void_beam");
		for (int i = 0; i < 500; i++) assertTrue(state.applyExplicitFeedback(true));

		assertEquals(0.0, state.rewardModifier("far:boss", "boss", "void_beam"), 1.0E-9);
		assertEquals(0.10, state.feedbackModifier("void_beam"), 1.0E-9);
		assertEquals(0.10, state.modifier("far:boss", "boss", "void_beam"), 1.0E-9);
		assertTrue(state.hasRememberedChoice());
	}

	@Test
	void feedbackStorageIsBoundedAndVersionedPersistenceRetainsLastChoice() {
		ShadowLearningState state = new ShadowLearningState();
		for (int i = 0; i < 40; i++) {
			state.rememberChoice("ctx" + i, "type" + i, "action" + i);
			assertTrue(state.applyExplicitFeedback(i % 2 == 0));
		}
		assertEquals(ShadowLearningState.MAX_FEEDBACK_ACTIONS, state.feedbackActionCount());

		ShadowLearningState decoded = ShadowLearningState.decode(state.encode());
		assertEquals(state.feedbackModifier("action39"), decoded.feedbackModifier("action39"),
				1.0E-9);
		assertTrue(decoded.hasRememberedChoice());
		assertTrue(decoded.applyExplicitFeedback(true));
	}

	@Test
	void legacyCombatLearningStillDecodesWithoutCreatingFeedback() {
		String raw = "v1;C,ctx,starfall,4,0.5,0;T,boss,starfall,4,0.25,0";
		String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(
				raw.getBytes(StandardCharsets.UTF_8));
		ShadowLearningState decoded = ShadowLearningState.decode(encoded);

		assertEquals(0.09375, decoded.rewardModifier("ctx", "boss", "starfall"), 1.0E-9);
		assertEquals(0.0, decoded.feedbackModifier("starfall"), 1.0E-9);
		assertFalse(decoded.hasRememberedChoice());
	}
}

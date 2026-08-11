package com.powers.companion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ShadowDialogueEngineTest {
	@Test
	void mechanicsStayTruthfulWhileTheVoiceRetainsAQuietAgenda() {
		ShadowDialogueEngine engine = new ShadowDialogueEngine();
		String failure = engine.failure("no_target");
		assertTrue(failure.toLowerCase().contains("target"));
		String accepted = engine.accepted(ShadowRequest.simple(ShadowRequest.Kind.GUARD, "owner"));
		assertTrue(accepted.toLowerCase().contains("guard"));
		assertTrue(accepted.toLowerCase().contains("dark"));
	}
}

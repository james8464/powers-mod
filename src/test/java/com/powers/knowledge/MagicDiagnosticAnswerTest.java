package com.powers.knowledge;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MagicDiagnosticAnswerTest {
	@Test
	void reportsExactEnergyFactsDeterministically() {
		MagicAttempt attempt = MagicAttempt.failure("fireball",
				MagicFailureReason.INSUFFICIENT_ENERGY, 10,
				Map.of("required", 40L, "available", 12L));
		assertEquals("Your Fireball failed because it required 40 energy, but only 12 was available. "
				+ "I recorded this at server tick 10.",
				MagicDiagnosticAnswer.text(attempt));
	}

	@Test
	void typedFactsCannotExposeCoordinatesOrIdentity() {
		MagicAttempt attempt = MagicAttempt.failure("teleport",
				MagicFailureReason.CONSENT, 10,
				Map.of("x", 123L, "target_uuid", 99L));
		assertEquals(Map.of(), attempt.facts());
		assertEquals("Your Teleport failed because the target did not grant consent. "
				+ "I recorded this at server tick 10.",
				MagicDiagnosticAnswer.text(attempt));
	}

	@Test
	void cooldownTicksAreRoundedUpToWholeSeconds() {
		MagicAttempt attempt = MagicAttempt.failure("void_beam",
				MagicFailureReason.COOLDOWN, 10, Map.of("remaining_ticks", 41L));
		assertEquals("Your Void Beam failed because its cooldown has 3 seconds remaining. "
				+ "I recorded this at server tick 10.",
				MagicDiagnosticAnswer.text(attempt));
	}
}

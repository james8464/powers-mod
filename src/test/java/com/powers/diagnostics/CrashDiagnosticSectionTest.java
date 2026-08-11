package com.powers.diagnostics;

import com.powers.knowledge.MagicAttempt;
import com.powers.knowledge.MagicFailureReason;
import com.powers.knowledge.MagicAttemptJournal;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrashDiagnosticSectionTest {
	@Test
	void sectionIsBoundedAggregateOnlyAndUsesTypedFailure() {
		Map<String, Integer> sessions = new LinkedHashMap<>();
		for (int index = 0; index < 40; index++) sessions.put("session_" + index, index);
		MagicAttempt failure = MagicAttempt.failure("powers:teleport\n/player-name",
				MagicFailureReason.SAFE_ZONE, 90, Map.of("distance", 12L));

		CrashDiagnosticSection section = CrashDiagnosticSection.create(sessions, failure);

		assertTrue(section.activeSessions().length() <= 256);
		assertTrue(section.lastTypedFailure().length() <= 160);
		assertTrue(section.lastTypedFailure().contains("safe_zone"));
		assertFalse(section.lastTypedFailure().contains("player-name"));
		assertFalse(section.lastTypedFailure().contains("\n"));
	}

	@Test
	void globalFailureIsTypedAndClearedWithTheServerSession() {
		MagicAttemptJournal journal = new MagicAttemptJournal();
		journal.record(java.util.UUID.randomUUID(), MagicAttempt.failure("player/private/action",
				MagicFailureReason.SAFE_ZONE, 42L, Map.of()));
		assertEquals(MagicFailureReason.SAFE_ZONE,
				journal.latestGlobalFailure().orElseThrow().reason());
		journal.clear();
		assertTrue(journal.latestGlobalFailure().isEmpty());
	}
}

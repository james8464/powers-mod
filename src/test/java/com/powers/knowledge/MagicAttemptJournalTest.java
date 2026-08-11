package com.powers.knowledge;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MagicAttemptJournalTest {
	@Test
	void evictsOldEntriesAndExpiresFailuresAfterFiveMinutes() {
		MagicAttemptJournal journal = new MagicAttemptJournal();
		UUID owner = UUID.randomUUID();
		for (int index = 0; index < 18; index++) {
			journal.record(owner, MagicAttempt.failure("action_" + index,
					MagicFailureReason.NO_TARGET, index, Map.of()));
		}
		assertEquals(16, journal.size(owner));
		assertEquals("action_17", journal.latestFailure(owner,
				"why did that fail", 18).orElseThrow().actionId());
		assertTrue(journal.latestFailure(owner, "why did that fail", 6_018).isEmpty());
	}

	@Test
	void aNamedQuestionSelectsThatActionInsteadOfTheLatestUnrelatedFailure() {
		MagicAttemptJournal journal = new MagicAttemptJournal();
		UUID owner = UUID.randomUUID();
		journal.record(owner, MagicAttempt.failure("fireball",
				MagicFailureReason.INSUFFICIENT_ENERGY, 20,
				Map.of("required", 40L, "available", 12L)));
		journal.record(owner, MagicAttempt.failure("teleport",
				MagicFailureReason.CONSENT, 21, Map.of()));

		MagicAttempt selected = journal.latestFailure(owner,
				"Shadow, why did my fireball not work?", 22).orElseThrow();
		assertEquals("fireball", selected.actionId());
		assertEquals(40L, selected.facts().get("required"));
	}

	@Test
	void successfulAttemptsDoNotReplaceTheLatestFailureDiagnosis() {
		MagicAttemptJournal journal = new MagicAttemptJournal();
		UUID owner = UUID.randomUUID();
		journal.record(owner, MagicAttempt.failure("fireball",
				MagicFailureReason.COOLDOWN, 30, Map.of("remaining_ticks", 41L)));
		journal.record(owner, MagicAttempt.success("lightning_strike", 31));
		assertEquals("fireball", journal.latestFailure(owner,
				"why did that fail", 32).orElseThrow().actionId());
	}

	@Test
	void thirdIdenticalFailureOffersOneRateLimitedHint() {
		MagicAttemptJournal journal = new MagicAttemptJournal();
		UUID owner = UUID.randomUUID();
		assertFalse(journal.record(owner, MagicAttempt.failure("fireball",
				MagicFailureReason.NO_TARGET, 1, Map.of())));
		assertFalse(journal.record(owner, MagicAttempt.failure("fireball",
				MagicFailureReason.NO_TARGET, 2, Map.of())));
		assertTrue(journal.record(owner, MagicAttempt.failure("fireball",
				MagicFailureReason.NO_TARGET, 3, Map.of())));
		assertFalse(journal.record(owner, MagicAttempt.failure("fireball",
				MagicFailureReason.NO_TARGET, 4, Map.of())));
	}
}

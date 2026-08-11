package com.powers.mind;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticipantPowerLockTest {
	@AfterEach
	void clearLocks() {
		ParticipantPowerLock.clear();
	}

	@Test
	void oneSessionLocksEveryParticipantAndRejectsNestedSessions() {
		UUID firstSession = UUID.randomUUID();
		UUID secondSession = UUID.randomUUID();
		UUID caster = UUID.randomUUID();
		UUID target = UUID.randomUUID();

		assertTrue(ParticipantPowerLock.acquire(firstSession, List.of(caster, target)));
		assertTrue(ParticipantPowerLock.isLocked(caster));
		assertTrue(ParticipantPowerLock.isLocked(target));
		assertFalse(ParticipantPowerLock.acquire(secondSession, List.of(target, UUID.randomUUID())));
	}

	@Test
	void releasingASessionUnlocksOnlyItsParticipants() {
		UUID firstSession = UUID.randomUUID();
		UUID secondSession = UUID.randomUUID();
		UUID first = UUID.randomUUID();
		UUID second = UUID.randomUUID();

		assertTrue(ParticipantPowerLock.acquire(firstSession, List.of(first)));
		assertTrue(ParticipantPowerLock.acquire(secondSession, List.of(second)));
		ParticipantPowerLock.release(firstSession);

		assertFalse(ParticipantPowerLock.isLocked(first));
		assertTrue(ParticipantPowerLock.isLocked(second));
	}

	@Test
	void duplicateAndEmptyParticipantSetsAreRejectedWithoutPartialLocks() {
		UUID session = UUID.randomUUID();
		UUID participant = UUID.randomUUID();

		assertFalse(ParticipantPowerLock.acquire(session, List.of()));
		assertFalse(ParticipantPowerLock.acquire(session, List.of(participant, participant)));
		assertFalse(ParticipantPowerLock.isLocked(participant));
	}
}

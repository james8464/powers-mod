package com.powers.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class LongLivedSummonRecordTest {
	private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000014");
	private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000015");

	@Test
	void activeRecordContainsOnlyStableAuthoritativeFacts() {
		LongLivedSummonRecord record = LongLivedSummonRecord.create(ID, OWNER,
				LongLivedSummonRecord.Task.GUARD,
				LongLivedSummonRecord.Archetype.ELITE, 1_000L, 1_200);

		assertEquals(ID, record.stableId());
		assertEquals(OWNER, record.ownerId());
		assertEquals(LongLivedSummonRecord.Task.GUARD, record.task());
		assertEquals(LongLivedSummonRecord.Archetype.ELITE, record.archetype());
		assertEquals(2_200L, record.expiresAtGameTime());
		assertFalse(record.expiredAt(2_199L));
		assertTrue(record.expiredAt(2_200L));
	}

	@Test
	void legacyRemainingLifetimeMigratesToBoundedAbsoluteExpiry() {
		LongLivedSummonRecord migrated = LongLivedSummonRecord.fromLegacy(
				ID, null, Integer.MAX_VALUE, false, 50_000L);

		assertEquals(LongLivedSummonRecord.Task.INVADE, migrated.task());
		assertEquals(LongLivedSummonRecord.Archetype.NORMAL, migrated.archetype());
		assertEquals(122_000L, migrated.expiresAtGameTime());
	}

	@Test
	void legacyZeroLifetimeCannotBecomeAnImmortalNaturalMob() {
		LongLivedSummonRecord migrated = LongLivedSummonRecord.fromLegacy(
				ID, OWNER, 0, false, 50_000L);

		assertTrue(migrated.expiredAt(50_000L));
	}

	@Test
	void compactIdsRoundTripWithoutPersistingDerivedIndexes() {
		for (LongLivedSummonRecord.Task task : LongLivedSummonRecord.Task.values()) {
			assertEquals(task, LongLivedSummonRecord.Task.fromId(task.id()).orElseThrow());
		}
		for (LongLivedSummonRecord.Archetype archetype
				: LongLivedSummonRecord.Archetype.values()) {
			assertEquals(archetype,
					LongLivedSummonRecord.Archetype.fromId(archetype.id()).orElseThrow());
		}
		assertTrue(LongLivedSummonRecord.Task.fromId((byte) 99).isEmpty());
		assertTrue(LongLivedSummonRecord.Archetype.fromId((byte) 99).isEmpty());
	}

	@Test
	void ownerAndTaskMustDescribeOneConsistentAuthority() {
		assertThrows(IllegalArgumentException.class, () -> new LongLivedSummonRecord(
				ID, null, LongLivedSummonRecord.Task.GUARD,
				LongLivedSummonRecord.Archetype.NORMAL, 100L));
		assertThrows(IllegalArgumentException.class, () -> new LongLivedSummonRecord(
				ID, OWNER, LongLivedSummonRecord.Task.INVADE,
				LongLivedSummonRecord.Archetype.NORMAL, 100L));
	}
}

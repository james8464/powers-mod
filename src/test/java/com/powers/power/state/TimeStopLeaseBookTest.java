package com.powers.power.state;

import com.powers.time.ControlTick;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeStopLeaseBookTest {
	private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");

	@Test
	void staleTokenAndWrongSourceCannotReleaseCurrentLease() {
		TimeStopLeaseBook book = new TimeStopLeaseBook();
		TimeStopLease lease = book.acquire(OWNER, TimeStopLeaseSource.CRYSTAL,
				ControlTick.at(10L), 40L, null, false).orElseThrow();

		assertFalse(book.release(lease.token() - 1L, OWNER,
				TimeStopLeaseSource.CRYSTAL, true).matched());
		assertFalse(book.release(lease.token(), OWNER,
				TimeStopLeaseSource.INNATE, true).matched());
		assertEquals(lease, book.active().orElseThrow());

		TimeStopLeaseBook.ReleaseDecision released = book.release(lease.token(), OWNER,
				TimeStopLeaseSource.CRYSTAL, true);
		assertTrue(released.matched());
		assertTrue(released.unfreeze());
		assertTrue(book.active().isEmpty());
	}

	@Test
	void externalWriteSupersedesLeaseAndPreservesFrozenState() {
		TimeStopLeaseBook book = new TimeStopLeaseBook();
		TimeStopLease lease = book.acquire(OWNER, TimeStopLeaseSource.INNATE,
				ControlTick.at(10L), Long.MAX_VALUE, null, false).orElseThrow();
		assertTrue(book.observeExternalWrite(() -> true));

		assertTrue(book.active().orElseThrow().externallySuperseded());
		TimeStopLeaseBook.ReleaseDecision retired = book.release(lease.token(), OWNER,
				TimeStopLeaseSource.INNATE, true);
		assertTrue(retired.matched());
		assertFalse(retired.unfreeze());
	}

	@Test
	void externalWriteAlwaysSupersedesProcessAuthorityEvenWhenJournalRetirementFails() {
		TimeStopLeaseBook book = new TimeStopLeaseBook();
		TimeStopLease lease = book.acquire(OWNER, TimeStopLeaseSource.INNATE,
				ControlTick.at(10L), Long.MAX_VALUE, null, false).orElseThrow();

		assertFalse(book.observeExternalWrite(() -> false));
		assertTrue(book.active().orElseThrow().externallySuperseded());
		assertFalse(book.release(lease.token(), OWNER,
				TimeStopLeaseSource.INNATE, true).unfreeze());

		TimeStopLease replacement = book.acquire(OWNER, TimeStopLeaseSource.INNATE,
				ControlTick.at(20L), Long.MAX_VALUE, null, false).orElseThrow();
		assertTrue(book.observeExternalWrite(() -> true));
		assertTrue(book.active().orElseThrow().externallySuperseded());
		assertEquals(replacement.token(), book.active().orElseThrow().token());
	}

	@Test
	void successiveAcquisitionsUseNewTokensAndRespectVanillaFreeze() {
		TimeStopLeaseBook book = new TimeStopLeaseBook();
		TimeStopLease first = book.acquire(OWNER, TimeStopLeaseSource.INNATE,
				ControlTick.at(1L), Long.MAX_VALUE, null, false).orElseThrow();
		assertTrue(book.acquire(OWNER, TimeStopLeaseSource.CRYSTAL,
				ControlTick.at(2L), 20L, null, false).isEmpty());
		book.release(first.token(), OWNER, TimeStopLeaseSource.INNATE, true);
		assertTrue(book.acquire(OWNER, TimeStopLeaseSource.CRYSTAL,
				ControlTick.at(3L), 20L, null, true).isEmpty());
		TimeStopLease second = book.acquire(OWNER, TimeStopLeaseSource.CRYSTAL,
				ControlTick.at(3L), 20L, null, false).orElseThrow();
		assertTrue(second.token() > first.token());
	}
}

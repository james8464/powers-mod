package com.powers.animation;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

final class CastingPoseLedgerTest {
	private static final UUID ONE = UUID.fromString("11111111-1111-1111-1111-111111111111");

	@Test
	void acceptedOfferCanBeSnapshottedWithoutRestartingItsClock() throws Exception {
		Class<?> type;
		try {
			type = Class.forName("com.powers.animation.CastingPoseLedger");
		} catch (ClassNotFoundException missing) {
			fail("CastingPoseLedger is not implemented");
			return;
		}
		Object ledger = type.getConstructor().newInstance();
		Object offered = type.getMethod("offer", int.class, UUID.class, CastingPose.class,
				CastingStyle.class, CastingHand.class, long.class, int.class)
				.invoke(ledger, 10, ONE, CastingPose.PROJECT, CastingStyle.RADIANT,
						CastingHand.RIGHT, 50L, 20);
		assertTrue((boolean) offered.getClass().getMethod("isPresent").invoke(offered));
		@SuppressWarnings("unchecked")
		Optional<CastingPoseEvent> snapshot = (Optional<CastingPoseEvent>) type
				.getMethod("snapshot", UUID.class, long.class).invoke(ledger, ONE, 55L);
		assertEquals(50L, snapshot.orElseThrow().startGameTime());
	}

	@Test
	void sameEntitySameTickReplacesWithoutSpendingTwoGlobalOffers() {
		var ledger = new CastingPoseLedger();
		assertTrue(offer(ledger, 1, ONE, 50L).isPresent());
		var replacement = ledger.offer(1, ONE, CastingPose.CHANNEL, CastingStyle.RADIANT,
				CastingHand.BOTH, 50L, 40).orElseThrow();
		assertEquals(2L, replacement.sequence());
		for (int index = 0; index < 63; index++) {
			assertTrue(offer(ledger, index + 2, uuid(index + 2), 50L).isPresent());
		}
		assertTrue(offer(ledger, 66, uuid(66), 50L).isEmpty());
	}

	@Test
	void activeCapacityRejectsNewIdentityWithoutEvictingLivePose() {
		var ledger = new CastingPoseLedger();
		for (int index = 0; index < CastingPoseLedger.MAX_ACTIVE; index++) {
			long tick = index / CastingPoseLedger.MAX_OFFERS_PER_TICK;
			assertTrue(ledger.offer(index, uuid(index), CastingPose.CHANNEL,
					CastingStyle.FIRST_VESSEL, CastingHand.BOTH, tick, 120).isPresent());
		}
		assertTrue(ledger.offer(999, uuid(999), CastingPose.INVOKE,
				CastingStyle.FIRST_VESSEL, CastingHand.BOTH, 4L, 20).isEmpty());
		assertTrue(ledger.snapshot(uuid(0), 4L).isPresent());
	}

	@Test
	void cleanupPreservesSequenceForLiveEntityAndResetsRemovedIdentity() {
		var ledger = new CastingPoseLedger();
		assertEquals(1L, offer(ledger, 1, ONE, 10L).orElseThrow().sequence());
		ledger.tick(40L, uuid -> true);
		assertEquals(2L, offer(ledger, 1, ONE, 40L).orElseThrow().sequence());
		ledger.tick(41L, uuid -> false);
		assertEquals(1L, offer(ledger, 1, ONE, 41L).orElseThrow().sequence());
	}

	@Test
	void sequenceExhaustionFailsClosedInsteadOfWrapping() throws Exception {
		var method = CastingPoseLedger.class.getMethod("nextSequence", long.class);
		assertEquals(OptionalLong.of(1L), method.invoke(null, 0L));
		assertEquals(OptionalLong.empty(), method.invoke(null, Long.MAX_VALUE));
	}

	@Test
	void metricsExposeAcceptedAndBoundedRejections() throws Exception {
		var ledger = new CastingPoseLedger();
		for (int index = 0; index < 65; index++) offer(ledger, index, uuid(index), 8L);
		Object metrics = CastingPoseLedger.class.getMethod("metrics").invoke(ledger);
		assertEquals(64L, metrics.getClass().getMethod("accepted").invoke(metrics));
		assertEquals(1L, metrics.getClass().getMethod("rejectedTickBudget").invoke(metrics));
		assertEquals(64, metrics.getClass().getMethod("activeEntries").invoke(metrics));
	}

	@Test
	void explicitChannelClearRemovesOnlyThatEntityPose() throws Exception {
		var ledger = new CastingPoseLedger();
		UUID two = uuid(2);
		offer(ledger, 1, ONE, 8L);
		offer(ledger, 2, two, 8L);
		try {
			CastingPoseLedger.class.getMethod("clear", UUID.class).invoke(ledger, ONE);
		} catch (NoSuchMethodException missing) {
			fail("CastingPoseLedger.clear(UUID) is not implemented");
		}
		assertTrue(ledger.snapshot(ONE, 8L).isEmpty());
		assertTrue(ledger.snapshot(two, 8L).isPresent());
	}

	private static Optional<CastingPoseEvent> offer(CastingPoseLedger ledger, int entityId,
			UUID uuid, long tick) {
		return ledger.offer(entityId, uuid, CastingPose.PROJECT, CastingStyle.RADIANT,
				CastingHand.RIGHT, tick, 20);
	}

	private static UUID uuid(int value) {
		return new UUID(0x1234L, value + 1L);
	}
}

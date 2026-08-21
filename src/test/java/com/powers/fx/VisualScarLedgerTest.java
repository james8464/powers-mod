package com.powers.fx;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualScarLedgerTest {
	private static final UUID FIRST = new UUID(0, 1);
	private static final UUID SECOND = new UUID(0, 2);
	private static final UUID THIRD = new UUID(0, 3);

	@Test
	void activeAndQueuedOwnerGlobalCapsAreIndependent() {
		var limits = VisualScarRules.Limits.hardCeilings();
		assertEquals(VisualScarLedgerRules.Admission.ALLOW,
				VisualScarLedgerRules.reserve(127, 2_047, 127, 2_047, limits));
		assertEquals(VisualScarLedgerRules.Admission.DENY_ACTIVE_OWNER,
				VisualScarLedgerRules.reserve(128, 100, 0, 0, limits));
		assertEquals(VisualScarLedgerRules.Admission.DENY_ACTIVE_GLOBAL,
				VisualScarLedgerRules.reserve(10, 2_048, 0, 0, limits));
		assertEquals(VisualScarLedgerRules.Admission.DENY_QUEUE_OWNER,
				VisualScarLedgerRules.reserve(10, 100, 128, 100, limits));
		assertEquals(VisualScarLedgerRules.Admission.DENY_QUEUE_GLOBAL,
				VisualScarLedgerRules.reserve(10, 100, 10, 2_048, limits));
	}

	@Test
	void hierarchicalRequestQueueAdvancesSparseDimensionsOwnersPoliciesAndImpacts() {
		var queue = new VisualScarRequestQueue(2_048, 128);
		for (int ownerIndex = 10; ownerIndex < 13; ownerIndex++) {
			UUID owner = new UUID(0, ownerIndex);
			for (long policy = 0; policy < 3; policy++) {
				for (var impact : VisualScarRules.Impact.values()) {
					assertTrue(queue.offer(request("overworld", policy, owner, impact)));
				}
			}
		}
		assertTrue(queue.offer(request("the_nether", 99, SECOND, VisualScarRules.Impact.ICE)));
		var rotated = new ArrayList<VisualScarLedgerRules.Request>();
		int sparseOffered = 1;
		for (int round = 0; round < 32; round++) {
			var batch = queue.poll(2);
			rotated.addAll(batch);
			assertTrue(queue.offer(request("overworld", round % 3,
					new UUID(0, 10 + round), VisualScarRules.Impact.values()[round % 5])));
			if (round > 0 && round % 4 == 0) {
				assertTrue(queue.offer(request("the_nether", 99, SECOND, VisualScarRules.Impact.ICE)));
				sparseOffered++;
			}
			if (round == 0) assertEquals(Set.of("minecraft:overworld", "minecraft:the_nether"),
					batch.stream().map(VisualScarLedgerRules.Request::dimension)
							.collect(java.util.stream.Collectors.toSet()));
		}
		assertTrue(rotated.stream().anyMatch(request -> request.owner().equals(SECOND)));
		assertEquals(sparseOffered, rotated.stream()
				.filter(request -> request.dimension().equals("minecraft:the_nether")).count());
		assertTrue(rotated.stream().map(VisualScarLedgerRules.Request::providerPolicyId).distinct().count() >= 3);
		assertTrue(rotated.stream().map(VisualScarLedgerRules.Request::impact).distinct().count() >= 5);
		assertTrue(queue.lastPollWork() <= 2);
		assertThrows(IllegalArgumentException.class, () -> queue.poll(65));
	}

	@Test
	void intrusiveKeyRingHasOneNodePerMemberAndCoversContinuousMembersUnderMaximumChurn() {
		Set<Long> members = LongStream.range(0, 2_048).boxed()
				.collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
		var ring = VisualScarRevalidationRing.of(members, 2_048);
		assertFalse(ring.insert(1L));
		Set<Long> inspected = new HashSet<>();
		for (int tick = 0; tick < 32; tick++) {
			for (int offset = 0; offset < 64; offset++) {
				long removed = tick < 16 ? 1_024L + tick * 64L + offset
						: 10_000L + (tick - 16L) * 64L + offset;
				assertTrue(ring.remove(removed));
				members.remove(removed);
				long inserted = tick < 16 ? 10_000L + tick * 64L + offset
						: 20_000L + tick * 64L + offset;
				assertTrue(ring.insert(inserted));
				members.add(inserted);
			}
			long cycled = tick < 16 ? 10_000L + tick * 64L : 20_000L + tick * 64L;
			assertTrue(ring.remove(cycled));
			assertTrue(ring.insert(cycled));
			var batch = ring.inspectNext(64);
			assertTrue(batch.size() <= 64);
			assertEquals(batch.size(), new HashSet<>(batch).size());
			inspected.addAll(batch);
			assertEquals(members.size(), ring.membershipSize());
			assertEquals(ring.membershipSize(), ring.physicalNodeCount());
			assertTrue(ring.physicalNodeCount() <= 2_048);
			assertTrue(ring.linksAreExact());
		}
		Set<Long> continuouslyPresent = LongStream.range(0, 1_024).boxed()
				.collect(java.util.stream.Collectors.toSet());
		assertTrue(inspected.containsAll(continuouslyPresent));
		assertEquals(members.size(), ring.membershipSize());
		assertEquals(ring.membershipSize(), ring.physicalNodeCount());
		assertThrows(IllegalArgumentException.class, () -> ring.inspectNext(65));
		assertThrows(IllegalArgumentException.class,
				() -> VisualScarRevalidationRing.of(List.of(1L), 2_049));
	}

	@Test
	void revalidationNeverLoadsAndOnlyRemovesChangedLoadedSupport() {
		var record = record(7, FIRST, 100);
		assertEquals(VisualScarLedgerRules.Revalidation.RETAIN_UNLOADED,
				VisualScarLedgerRules.revalidate(record, false, true, false, 0xAAL));
		assertEquals(VisualScarLedgerRules.Revalidation.RETAIN_UNLOADED,
				VisualScarLedgerRules.revalidate(record, true, false, false, 0xAAL));
		assertEquals(VisualScarLedgerRules.Revalidation.RETAIN,
				VisualScarLedgerRules.revalidate(record, true, true, true, 0xAAL));
		assertEquals(VisualScarLedgerRules.Revalidation.REMOVE_STALE,
				VisualScarLedgerRules.revalidate(record, true, true, true, 0xBBL));
		assertEquals(VisualScarLedgerRules.Revalidation.REMOVE_STALE,
				VisualScarLedgerRules.revalidate(record, true, true, false, 0xAAL));
	}

	@Test
	void treeExpiryIsBoundedAndSkipsFutureBuckets() {
		var index = new VisualScarExpiryIndex<Long>(2_048);
		for (long key = 0; key < 100; key++) assertTrue(index.put(key, 50));
		assertTrue(index.put(1_000L, 500));
		var indexedDue = index.pollDue(100, 64);
		assertEquals(64, indexedDue.keys().size());
		assertEquals(64, indexedDue.inspected());
		assertTrue(index.contains(1_000L));
		assertEquals(37, index.size());
		assertTrue(index.put(1_000L, 40));
		assertEquals(List.of(1_000L), index.pollDue(100, 1).keys());

		var capped = new VisualScarExpiryIndex<Long>(2);
		assertTrue(capped.put(1L, 10));
		assertTrue(capped.put(2L, 10));
		assertFalse(capped.put(3L, 10));
		assertThrows(IllegalArgumentException.class, () -> index.pollDue(100, 65));
		assertThrows(IllegalArgumentException.class, () -> index.pollDue(-1, 1));
		assertThrows(IllegalArgumentException.class,
				() -> new VisualScarLedgerRules.Record("minecraft:overworld", 1,
						VisualScarRules.Face.UP, FIRST, VisualScarRules.Impact.BEAM,
						VisualScarRules.Material.STONE, 1, 1, 1, 0, 39));
		assertThrows(IllegalArgumentException.class,
				() -> new VisualScarLedgerRules.Record("minecraft:overworld", 1,
						VisualScarRules.Face.UP, FIRST, VisualScarRules.Impact.BEAM,
						VisualScarRules.Material.STONE, 1, 1, 1, 0, 1_201));
		assertThrows(IllegalArgumentException.class,
				() -> new VisualScarLedgerRules.Record(" ", 1,
						VisualScarRules.Face.UP, FIRST, VisualScarRules.Impact.BEAM,
						VisualScarRules.Material.STONE, 1, 1, 1, 0, 40));
	}

	@Test
	void movementAndTeleportIntoRangeStartBoundedResyncWithoutCopies() {
		var records = LongStream.range(0, 300).mapToObj(index -> record(index, FIRST, 500)).toList();
		var session = new VisualScarLedgerRules.ObserverSession(FIRST, 91,
				"minecraft:overworld", 4);
		var cursor = VisualScarLedgerRules.observeMovement(session, 2_000, 0, 0,
				0, 0, 0, false);
		assertTrue(cursor.needsResync());
		assertEquals(0, cursor.materializedRecords());
		assertEquals(300, records.size());
	}

	@Test
	void midPageDimensionReconnectAndExpiryInvalidateStaleTail() {
		var old = new VisualScarLedgerRules.ObserverSession(FIRST, 91,
				"minecraft:overworld", 4);
		var changedDimension = new VisualScarLedgerRules.ObserverSession(FIRST, 91,
				"minecraft:the_nether", 5);
		var reconnected = new VisualScarLedgerRules.ObserverSession(FIRST, 92,
				"minecraft:overworld", 6);
		assertFalse(VisualScarLedgerRules.sessionCurrent(old, changedDimension));
		assertFalse(VisualScarLedgerRules.sessionCurrent(old, reconnected));
		assertEquals(0, VisualScarLedgerRules.observeMovement(old,
				0, 0, 0, 0, 0, 0, false).materializedRecords());
	}

	private static VisualScarLedgerRules.Request request(String dimension, long policy, UUID owner,
			VisualScarRules.Impact impact) {
		return new VisualScarLedgerRules.Request("minecraft:" + dimension, policy, owner, impact);
	}

	private static VisualScarLedgerRules.Record record(long position, UUID owner, long expiry) {
		return new VisualScarLedgerRules.Record("minecraft:overworld", position,
				VisualScarRules.Face.UP, owner, VisualScarRules.Impact.BEAM,
				VisualScarRules.Material.STONE, (int) position, position + 1,
				0xAAL, 0, expiry);
	}
}

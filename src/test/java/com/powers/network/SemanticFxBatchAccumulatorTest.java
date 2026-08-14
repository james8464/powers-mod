package com.powers.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SemanticFxBatchAccumulatorTest {
	@Test
	void preservesOneImmediateCueAndBatchesOnlyTheMeasuredCompressibleTail() {
		SemanticFxBatchAccumulator<String> accumulator = new SemanticFxBatchAccumulator<>();

		assertEquals(SemanticFxBatchAccumulator.Delivery.IMMEDIATE,
				accumulator.offer(1L, "connection-a", "lead", 64).delivery());
		assertEquals(SemanticFxBatchAccumulator.Delivery.DEFERRED,
				accumulator.offer(1L, "connection-a", "two", 64).delivery());
		assertEquals(SemanticFxBatchAccumulator.Delivery.DEFERRED,
				accumulator.offer(1L, "connection-a", "three", 64).delivery());
		assertEquals(SemanticFxBatchAccumulator.Delivery.DEFERRED,
				accumulator.offer(1L, "connection-a", "four", 64).delivery());

		var small = accumulator.drain();
		assertEquals(java.util.List.of("two", "three", "four"), small.entries());

		assertEquals(SemanticFxBatchAccumulator.Delivery.IMMEDIATE,
				accumulator.offer(2L, "connection-a", "next-lead", 64).delivery());
		for (int index = 0; index < 4; index++) {
			assertEquals(SemanticFxBatchAccumulator.Delivery.DEFERRED,
					accumulator.offer(2L, "connection-a", "tail-" + index, 64).delivery());
		}
		var compressible = accumulator.drain();
		assertEquals(256, compressible.encodedBytes());
	}

	@Test
	void capacityRolloverReturnsTheEarlierTailBeforeTheNewImmediateCue() {
		SemanticFxBatchAccumulator<Integer> accumulator = new SemanticFxBatchAccumulator<>(2);
		assertEquals(SemanticFxBatchAccumulator.Delivery.IMMEDIATE,
				accumulator.offer(1L, "connection-a", 0, 64).delivery());
		assertEquals(SemanticFxBatchAccumulator.Delivery.DEFERRED,
				accumulator.offer(1L, "connection-a", 1, 64).delivery());
		assertEquals(SemanticFxBatchAccumulator.Delivery.DEFERRED,
				accumulator.offer(1L, "connection-a", 2, 64).delivery());

		var rollover = accumulator.offer(1L, "connection-a", 3, 64);
		assertEquals(SemanticFxBatchAccumulator.Delivery.IMMEDIATE, rollover.delivery());
		assertEquals(SemanticFxBatchAccumulator.Rollover.CAPACITY, rollover.rollover());
		assertEquals(java.util.List.of(1, 2), rollover.before().entries());
		assertTrue(accumulator.drain().entries().isEmpty());
	}

	@Test
	void tickRolloverFlushesButConnectionRolloverMarksStalePresentation() {
		SemanticFxBatchAccumulator<String> accumulator = new SemanticFxBatchAccumulator<>();
		accumulator.offer(10L, "connection-a", "lead-a", 64);
		accumulator.offer(10L, "connection-a", "tail-a", 64);

		var nextTick = accumulator.offer(11L, "connection-a", "lead-b", 64);
		assertEquals(SemanticFxBatchAccumulator.Rollover.TICK, nextTick.rollover());
		assertEquals(java.util.List.of("tail-a"), nextTick.before().entries());
		assertEquals(SemanticFxBatchAccumulator.Delivery.IMMEDIATE, nextTick.delivery());

		accumulator.offer(11L, "connection-a", "tail-b", 64);
		var reconnect = accumulator.offer(11L, "connection-b", "lead-c", 64);
		assertEquals(SemanticFxBatchAccumulator.Rollover.CHANNEL, reconnect.rollover());
		assertEquals(java.util.List.of("tail-b"), reconnect.before().entries());
		assertEquals(SemanticFxBatchAccumulator.Delivery.IMMEDIATE, reconnect.delivery());
	}

	@Test
	void aMixedBurstBeyondThe128EntryLimitStillDeliversInExactOrder() {
		SemanticFxBatchAccumulator<Integer> accumulator = new SemanticFxBatchAccumulator<>();
		java.util.List<Integer> delivered = new java.util.ArrayList<>();
		for (int value = 0; value < 132; value++) {
			var offer = accumulator.offer(20L, "connection-a", value, 64);
			delivered.addAll(offer.before().entries());
			if (offer.delivery() == SemanticFxBatchAccumulator.Delivery.IMMEDIATE) {
				delivered.add(value);
			}
		}
		delivered.addAll(accumulator.drain().entries());

		assertEquals(java.util.stream.IntStream.range(0, 132).boxed().toList(), delivered);
	}
}

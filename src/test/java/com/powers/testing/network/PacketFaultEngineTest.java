package com.powers.testing.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PacketFaultEngineTest {
	private static final PacketFaultConnection CONNECTION =
			new PacketFaultConnection(UUID.fromString("00000000-0000-0000-0000-000000000009"), 3L);

	@Test
	void disabledProfileExecutesInlineWithoutCreatingFaultWork() {
		PacketFaultEngine engine = new PacketFaultEngine(PacketFaultProfile.disabled());
		List<String> delivered = new ArrayList<>();

		PacketFaultEngine.OfferResult result = engine.offer(CONNECTION,
				PacketFaultDirection.SERVERBOUND, PacketFaultFamily.ARTIFACT_SELECTION,
				0L, delivered::add, () -> delivered.add("failed"), "latest");

		assertEquals(PacketFaultEngine.OfferResult.BYPASSED, result);
		assertEquals(List.of("latest"), delivered);
		assertEquals(PacketFaultMetrics.empty(), engine.snapshot());
		assertEquals(0, engine.queueDepth());
	}

	@Test
	void delayUsesGameTicksAndNeverWallClockTime() {
		PacketFaultEngine engine = new PacketFaultEngine(PacketFaultProfile.named("delay150", 91L));
		List<String> delivered = new ArrayList<>();
		engine.offer(CONNECTION, PacketFaultDirection.SERVERBOUND,
				PacketFaultFamily.GRIMOIRE_SELECTION, 20L, delivered::add, () -> { }, "spell");

		engine.tick(22L);
		assertTrue(delivered.isEmpty());
		engine.tick(23L);
		assertEquals(List.of("spell"), delivered);
		assertEquals(3L, engine.snapshot().maximumAgeTicks());
	}

	@Test
	void seededLossHasAnExactRepeatableCount() {
		PacketFaultProfile profile = new PacketFaultProfile("loss5-fixture", 0x5EEDL,
				EnumSet.allOf(PacketFaultDirection.class), EnumSet.allOf(PacketFaultFamily.class),
				0, 500, 0, 0, 20_000, 40, 20_000);
		PacketFaultEngine first = new PacketFaultEngine(profile);
		PacketFaultEngine second = new PacketFaultEngine(profile);
		List<Integer> firstDelivered = new ArrayList<>();
		List<Integer> secondDelivered = new ArrayList<>();
		for (int value = 0; value < 10_000; value++) {
			int packet = value;
			first.offer(CONNECTION, PacketFaultDirection.CLIENTBOUND,
					PacketFaultFamily.MAGIC_FX, 0L, firstDelivered::add, () -> { }, packet);
			second.offer(CONNECTION, PacketFaultDirection.CLIENTBOUND,
					PacketFaultFamily.MAGIC_FX, 0L, secondDelivered::add, () -> { }, packet);
		}
		for (long tick = 0L; tick < 3L; tick++) {
			first.tick(tick);
			second.tick(tick);
		}

		assertEquals(firstDelivered, secondDelivered);
		assertEquals(10_000L, first.snapshot().offered());
		assertEquals(522L, first.snapshot().dropped());
		assertEquals(9_478, firstDelivered.size());
	}

	@Test
	void familyAndDirectionFiltersBypassWithoutFaultAccounting() {
		PacketFaultProfile profile = new PacketFaultProfile("filtered", 8L,
				EnumSet.of(PacketFaultDirection.CLIENTBOUND),
				EnumSet.of(PacketFaultFamily.EVENT_AUDIO), 6, 10_000, 0, 0,
				16, 20, 4);
		PacketFaultEngine engine = new PacketFaultEngine(profile);
		List<String> delivered = new ArrayList<>();

		engine.offer(CONNECTION, PacketFaultDirection.SERVERBOUND,
				PacketFaultFamily.EVENT_AUDIO, 0L, delivered::add, () -> { }, "wrong-direction");
		engine.offer(CONNECTION, PacketFaultDirection.CLIENTBOUND,
				PacketFaultFamily.POWER_STATE, 0L, delivered::add, () -> { }, "wrong-family");

		assertEquals(List.of("wrong-direction", "wrong-family"), delivered);
		assertEquals(PacketFaultMetrics.empty(), engine.snapshot());
	}

	@Test
	void boundedQueueFailsClosedAndExpiresOldWork() {
		PacketFaultProfile profile = new PacketFaultProfile("bounded", 4L,
				EnumSet.allOf(PacketFaultDirection.class), EnumSet.allOf(PacketFaultFamily.class),
				20, 0, 0, 0, 2, 3, 1);
		PacketFaultEngine engine = new PacketFaultEngine(profile);
		List<String> delivered = new ArrayList<>();
		List<String> failed = new ArrayList<>();
		engine.offer(CONNECTION, PacketFaultDirection.SERVERBOUND,
				PacketFaultFamily.LOCATOR_REQUEST, 0L, delivered::add, () -> failed.add("one"), "one");
		engine.offer(CONNECTION, PacketFaultDirection.SERVERBOUND,
				PacketFaultFamily.LOCATOR_REQUEST, 0L, delivered::add, () -> failed.add("two"), "two");
		engine.offer(CONNECTION, PacketFaultDirection.SERVERBOUND,
				PacketFaultFamily.LOCATOR_REQUEST, 0L, delivered::add,
				() -> failed.add("overflow"), "overflow");

		assertEquals(List.of("overflow"), failed);
		engine.tick(4L);
		assertEquals(List.of("overflow", "one", "two"), failed);
		engine.tick(5L);
		assertEquals(List.of("overflow", "one", "two"), failed);
		assertTrue(delivered.isEmpty());
		assertEquals(1L, engine.snapshot().overflowed());
		assertEquals(2L, engine.snapshot().expired());
	}

	@Test
	void duplicateCopyOverflowNeverInvalidatesTheQueuedOriginal() {
		PacketFaultProfile profile = new PacketFaultProfile("duplicate-bound", 4L,
				EnumSet.allOf(PacketFaultDirection.class), EnumSet.allOf(PacketFaultFamily.class),
				1, 0, 10_000, 0, 1, 20, 1);
		PacketFaultEngine engine = new PacketFaultEngine(profile);
		List<String> delivered = new ArrayList<>();
		List<String> failures = new ArrayList<>();
		engine.offer(CONNECTION, PacketFaultDirection.SERVERBOUND, PacketFaultFamily.ARTIFACT_CAST,
				0L, delivered::add, () -> failures.add("invalidated"), "cast");
		engine.tick(1L);

		assertEquals(List.of("cast"), delivered);
		assertTrue(failures.isEmpty());
		assertEquals(1L, engine.snapshot().overflowed());
	}

	@Test
	void lifecycleCancellationInvokesFailClosedCallbacksAndAccountsThem() {
		PacketFaultEngine engine = new PacketFaultEngine(PacketFaultProfile.named("delay300", 9L));
		List<String> failures = new ArrayList<>();
		engine.offer(CONNECTION, PacketFaultDirection.SERVERBOUND, PacketFaultFamily.VESSEL_RELEASE,
				0L, ignored -> true, () -> failures.add("released"), "release");

		assertEquals(1, engine.clear(CONNECTION));
		assertEquals(List.of("released"), failures);
		assertEquals(1L, engine.snapshot().cancelled());
	}

	@Test
	void globalQueueAndTickWorkRemainHardBounded() {
		PacketFaultProfile profile = new PacketFaultProfile("global-bounds", 12L,
				EnumSet.allOf(PacketFaultDirection.class), EnumSet.allOf(PacketFaultFamily.class),
				10, 0, 0, 0, 1, 40, 10_000);
		PacketFaultEngine engine = new PacketFaultEngine(profile);
		for (int index = 0; index < 32_769; index++) {
			PacketFaultConnection connection = new PacketFaultConnection(new UUID(0L, index + 1L), 1L);
			engine.offer(connection, PacketFaultDirection.CLIENTBOUND, PacketFaultFamily.MAGIC_FX,
					0L, ignored -> true, () -> { }, index);
		}
		assertEquals(32_768, engine.queueDepth());
		assertEquals(1L, engine.snapshot().overflowed());

		engine.tick(10L);
		assertEquals(4_096L, engine.snapshot().delivered());
		assertEquals(32_768 - 4_096, engine.queueDepth());
	}

	@Test
	void perConnectionWorkAllowanceMakesFairProgress() {
		PacketFaultProfile profile = new PacketFaultProfile("fair-work", 13L,
				EnumSet.allOf(PacketFaultDirection.class), EnumSet.allOf(PacketFaultFamily.class),
				0, 0, 0, 0, 4, 40, 1);
		PacketFaultEngine engine = new PacketFaultEngine(profile);
		PacketFaultConnection other = new PacketFaultConnection(new UUID(0L, 99L), 1L);
		List<String> delivered = new ArrayList<>();
		for (String value : List.of("a1", "a2")) engine.offer(CONNECTION,
				PacketFaultDirection.CLIENTBOUND, PacketFaultFamily.MAGIC_FX,
				0L, delivered::add, () -> { }, value);
		for (String value : List.of("b1", "b2")) engine.offer(other,
				PacketFaultDirection.CLIENTBOUND, PacketFaultFamily.MAGIC_FX,
				0L, delivered::add, () -> { }, value);

		engine.tick(0L);
		assertEquals(2, delivered.size());
		assertTrue(delivered.stream().anyMatch(value -> value.startsWith("a")));
		assertTrue(delivered.stream().anyMatch(value -> value.startsWith("b")));
	}

	@Test
	void acceptanceMatrixIsDeterministicAndHasNoDuplicateAuthorityEffects() {
		for (String profileId : List.of("delay150", "delay300", "loss1", "loss5", "duplicate", "reorder")) {
			PacketFaultEngine engine = new PacketFaultEngine(PacketFaultProfile.named(profileId, 0x9A009L));
			PacketFaultFamily family = "reorder".equals(profileId)
					? PacketFaultFamily.POWER_STATE : PacketFaultFamily.ARTIFACT_CAST;
			for (int packet = 0; packet < 1_000; packet++) {
				engine.offer(CONNECTION, PacketFaultDirection.SERVERBOUND, family, packet,
						ignored -> true, () -> { }, packet);
				engine.tick(packet);
			}
			for (long tick = 1_000L; tick <= 1_050L; tick++) engine.tick(tick);
			PacketFaultMetrics metrics = engine.snapshot();
			assertEquals(0L, metrics.duplicateSideEffects(), profileId);
			assertEquals(0, engine.queueDepth(), profileId);
			System.out.println("QA009_MATRIX " + profileId + " seed=630793 offered=" + metrics.offered()
					+ " dropped=" + metrics.dropped() + " duplicated=" + metrics.duplicated()
					+ " delayed=" + metrics.delayed() + " reordered=" + metrics.reordered()
					+ " delivered=" + metrics.delivered() + " expired=" + metrics.expired()
					+ " overflowed=" + metrics.overflowed() + " stale=" + metrics.suppressedStale()
					+ " cancelled=" + metrics.cancelled() + " maxQueue=" + metrics.maximumQueueDepth()
					+ " maxAge=" + metrics.maximumAgeTicks());
		}
	}

	@Test
	void duplicateAndReorderCannotApplyAnOlderStatefulRequest() {
		PacketFaultEngine engine = new PacketFaultEngine(PacketFaultProfile.named("reorder", 42L));
		List<String> selected = new ArrayList<>();
		for (String value : List.of("first", "second", "latest")) {
			engine.offer(CONNECTION, PacketFaultDirection.SERVERBOUND,
					PacketFaultFamily.ARTIFACT_SELECTION, 0L, selected::add, () -> { }, value);
		}
		for (long tick = 0; tick <= 20; tick++) engine.tick(tick);

		assertFalse(selected.isEmpty());
		assertEquals("latest", selected.getLast());
		assertEquals(0L, engine.snapshot().duplicateSideEffects());
		assertTrue(engine.snapshot().reordered() > 0L);
	}

	@Test
	void latestWinsIsScopedToTheLogicalStateOwner() {
		PacketFaultEngine engine = new PacketFaultEngine(PacketFaultProfile.named("reorder", 42L));
		List<String> delivered = new ArrayList<>();
		engine.offer(CONNECTION, PacketFaultDirection.CLIENTBOUND, PacketFaultFamily.BODY_SNAPSHOT,
				"body:4", 0L, delivered::add, () -> { }, "body-four");
		engine.offer(CONNECTION, PacketFaultDirection.CLIENTBOUND, PacketFaultFamily.BODY_SNAPSHOT,
				"body:5", 0L, delivered::add, () -> { }, "body-five");

		for (long tick = 0L; tick <= 20L; tick++) engine.tick(tick);

		assertEquals(2, delivered.size());
		assertTrue(delivered.containsAll(List.of("body-four", "body-five")));
	}

	@Test
	void seededDecisionsAreRepeatableButIndependentAcrossLogicalStreams() {
		PacketFaultProfile profile = new PacketFaultProfile("stream-loss", 0x9009L,
				EnumSet.allOf(PacketFaultDirection.class), EnumSet.allOf(PacketFaultFamily.class),
				0, 5_000, 0, 0, 256, 40, 256);
		List<String> first = exerciseIndependentStreams(profile);
		List<String> second = exerciseIndependentStreams(profile);

		assertEquals(first, second);
		assertTrue(first.stream().anyMatch(value -> value.startsWith("body:4:")));
		assertTrue(first.stream().anyMatch(value -> value.startsWith("body:5:")));
		long bodyFourDelivered = first.stream().filter(value -> value.startsWith("body:4:delivered")).count();
		long bodyFiveDelivered = first.stream().filter(value -> value.startsWith("body:5:delivered")).count();
		assertNotEquals(bodyFourDelivered, bodyFiveDelivered,
				"Logical streams received perfectly correlated decisions");
	}

	private static List<String> exerciseIndependentStreams(PacketFaultProfile profile) {
		PacketFaultEngine engine = new PacketFaultEngine(profile);
		List<String> outcomes = new ArrayList<>();
		for (int index = 0; index < 64; index++) {
			int packet = index;
			for (String key : List.of("body:4", "body:5")) {
				engine.offer(CONNECTION, PacketFaultDirection.CLIENTBOUND, PacketFaultFamily.BODY_SNAPSHOT,
						key, 0L, ignored -> outcomes.add(key + ":delivered:" + packet),
						() -> outcomes.add(key + ":dropped:" + packet), packet);
			}
		}
		engine.tick(0L);
		return outcomes;
	}

	@Test
	void idleLogicalStreamMetadataIsReleasedAfterDelivery() {
		PacketFaultEngine engine = new PacketFaultEngine(PacketFaultProfile.named("duplicate", 33L));
		for (int index = 0; index < 20_000; index++) {
			engine.offer(CONNECTION, PacketFaultDirection.CLIENTBOUND, PacketFaultFamily.BODY_SNAPSHOT,
					"body:" + index, 0L, ignored -> true, () -> { }, index);
			engine.tick(0L);
		}

		assertEquals(0, engine.queueDepth());
		assertEquals(0, engine.retainedStreamCount());
	}

	@Test
	void duplicatedDiscreteCastRunsOnceWhileDistinctCastsArePreserved() {
		PacketFaultEngine engine = new PacketFaultEngine(PacketFaultProfile.named("duplicate", 19L));
		List<String> casts = new ArrayList<>();
		engine.offer(CONNECTION, PacketFaultDirection.SERVERBOUND,
				PacketFaultFamily.ARTIFACT_CAST, 0L, casts::add, () -> { }, "cast-one");
		engine.offer(CONNECTION, PacketFaultDirection.SERVERBOUND,
				PacketFaultFamily.ARTIFACT_CAST, 0L, casts::add, () -> { }, "cast-two");
		engine.tick(0L);

		assertEquals(List.of("cast-one", "cast-two"), casts);
		assertEquals(2L, engine.snapshot().duplicated());
		assertEquals(2L, engine.snapshot().suppressedStale());
		assertEquals(0L, engine.snapshot().duplicateSideEffects());
	}

	@Test
	void disconnectAndServerStopClearQueuedPayloadsAndCounters() {
		PacketFaultEngine engine = new PacketFaultEngine(PacketFaultProfile.named("delay300", 7L));
		List<String> delivered = new ArrayList<>();
		List<String> failed = new ArrayList<>();
		engine.offer(CONNECTION, PacketFaultDirection.SERVERBOUND,
				PacketFaultFamily.VESSEL_INPUT, 0L, delivered::add, () -> { }, "input");
		assertEquals(1, engine.clear(CONNECTION));
		engine.tick(10L);
		assertTrue(delivered.isEmpty());

		engine.offer(CONNECTION, PacketFaultDirection.CLIENTBOUND,
				PacketFaultFamily.POWER_STATE, 10L, delivered::add,
				() -> failed.add("refresh"), "state");
		engine.reset();
		assertEquals(List.of("refresh"), failed,
				"Reset must invoke fail-closed recovery before clearing its counters");
		assertEquals(PacketFaultMetrics.empty(), engine.snapshot());
		assertEquals(0, engine.queueDepth());
	}
}

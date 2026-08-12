package com.powers.performance;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerTickProfilerTest {
	@Test
	void percentilesUseNearestRankAndConvertNanosToMspt() {
		List<Long> samples = java.util.stream.LongStream.rangeClosed(1, 100)
				.map(value -> value * 1_000_000L).boxed().toList();
		assertEquals(95.0, ServerTickProfiler.percentileMs(samples, 0.95), 0.0001);
		assertEquals(99.0, ServerTickProfiler.percentileMs(samples, 0.99), 0.0001);
		assertEquals(1.0, ServerTickProfiler.percentileMs(samples, -1.0), 0.0001);
		assertEquals(100.0, ServerTickProfiler.percentileMs(samples, 2.0), 0.0001);
	}

	@Test
	void emptyProfilesHaveZeroPercentiles() {
		assertEquals(0.0, ServerTickProfiler.percentileMs(List.of(), 0.95));
	}

	@Test
	void realTimeHarnessUsesACumulativeTwentyTpsDeadlineWithoutDrift() {
		assertEquals(1_000_000_000L,
				ServerTickProfiler.profileDeadlineNanos(1_000_000_000L, 0));
		assertEquals(1_050_000_000L,
				ServerTickProfiler.profileDeadlineNanos(1_000_000_000L, 1));
		assertEquals(1_100_000_000L,
				ServerTickProfiler.profileDeadlineNanos(1_000_000_000L, 2));
	}

	@Test
	void duplicateLifecycleCallbacksCannotSampleOneLogicalTickTwice() {
		assertEquals(true, ServerTickProfiler.isNewLogicalTick(41L, 42L));
		assertEquals(false, ServerTickProfiler.isNewLogicalTick(42L, 42L));
		assertEquals(false, ServerTickProfiler.isNewLogicalTick(43L, 42L));
	}
}

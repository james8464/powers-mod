package com.powers.performance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contract tests for the exact before/after PERF-006 workload. */
class FxAllocationProfileTest {
	private static final double CAPTURED_BASELINE_BYTES_PER_OPERATION = 7_587.795;

	@Test
	void profileCountsSemanticOperationsAndReportsBoundedRetention() {
		FxAllocationProfile.Result result = FxAllocationProfile.measure(2, 4, 8);

		assertEquals(32, result.operations());
		assertTrue(result.allocatedBytes() > 0);
		assertTrue(result.allocatedBytesPerOperation() > 0.0);
		assertTrue(result.p99NanosPerOperation() > 0L);
		assertTrue(result.geometryEntries() <= 256);
		assertTrue(result.payloadEntries() <= 1_024);
		assertTrue(result.blackhole() != 0L);
	}

	@Test
	void productionHotPathAllocatesAtLeastTwentyPercentLessThanExactBaseline() {
		FxAllocationProfile.Result result = FxAllocationProfile.measure(10, 40, 32);

		assertTrue(result.allocatedBytesPerOperation()
				<= CAPTURED_BASELINE_BYTES_PER_OPERATION * 0.80,
				() -> "FX allocation regression: " + result.allocatedBytesPerOperation()
						+ " bytes/op");
	}
}

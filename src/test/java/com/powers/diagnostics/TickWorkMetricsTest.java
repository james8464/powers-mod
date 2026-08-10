package com.powers.diagnostics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TickWorkMetricsTest {
	@Test
	void countersAccumulateWithinTickAndResetAtNextTick() {
		TickWorkMetrics metrics = new TickWorkMetrics();
		metrics.recordParticles(10, 14);
		metrics.recordPackets(10, 2);
		metrics.recordEntityInspections(10, 31);
		metrics.recordPackets(10, 3);

		assertEquals(new TickWorkMetrics.Snapshot(10, 14, 5, 31), metrics.snapshot(10));
		assertEquals(new TickWorkMetrics.Snapshot(11, 0, 0, 0), metrics.snapshot(11));
	}

	@Test
	void negativeAmountsCannotCorruptDiagnostics() {
		TickWorkMetrics metrics = new TickWorkMetrics();
		metrics.recordParticles(4, -100);
		metrics.recordPackets(4, -1);
		metrics.recordEntityInspections(4, -5);

		assertEquals(new TickWorkMetrics.Snapshot(4, 0, 0, 0), metrics.snapshot(4));
	}
}

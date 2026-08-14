package com.powers.client;

import com.powers.fx.BeamFxStyle;
import com.powers.network.MagicFxPackets;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientSemanticFxMetricsTest {
	@Test
	void recordsIndividualAndBatchReceptionInDeliveryOrder() {
		ClientSemanticFxMetrics.reset();
		ClientSemanticFxMetrics.recordIndividual(10L);
		ClientSemanticFxMetrics.recordBatch(List.of(beam(11L), beam(12L)));

		assertEquals(new ClientSemanticFxMetrics.Snapshot(1, 1, 2, List.of(10L, 11L, 12L)),
				ClientSemanticFxMetrics.snapshot());
	}

	private static MagicFxPackets.BatchEntry beam(long eventId) {
		return MagicFxPackets.BatchEntry.beam(new MagicFxPackets.BeamFxPayload(eventId,
				BeamFxStyle.ELECTRIC, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 16, 0xABCDEF));
	}
}

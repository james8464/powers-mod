package com.powers.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FxCompressionProfileTest {
	static {
		net.minecraft.SharedConstants.tryDetectVersion();
		net.minecraft.server.Bootstrap.bootStrap();
	}

	@Test
	void actualMixedPayloadProfileMeasuresAndEnforcesTheRuntimeSelectionCriterion() {
		var result = FxCompressionProfile.measure(2, 8);

		assertEquals(java.util.List.of(64, 128, 256, 512, 1024, 2048, 4096, 8192),
				result.rows().stream().map(FxCompressionProfile.Row::targetBytes)
						.distinct().toList());
		assertEquals(java.util.List.of(-1, 128, 256, 512),
				result.rows().stream().map(FxCompressionProfile.Row::compressionThreshold)
						.distinct().sorted().toList());
		assertTrue(result.rows().stream().allMatch(FxCompressionProfile.Row::roundTrip));
		assertTrue(result.rows().stream().allMatch(row -> row.selected()
				== (row.entryCount() > 1 && row.batchWireBytes() < row.individualWireBytes())));
		assertTrue(result.rows().stream().allMatch(row -> row.decisionP95Nanos() > 0L));
		assertTrue(result.rows().stream().allMatch(row -> row.decodeP95Nanos() > 0L));
		assertTrue(result.rows().stream().anyMatch(row -> row.targetBytes() >= 256 && row.selected()));
		assertTrue(result.rows().stream().allMatch(row -> row.incompressibleControlSelected()
				== (row.incompressibleBatchWireBytes() < row.incompressibleIndividualWireBytes())));
	}
}

package com.powers.item;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactEnergyReservoirTest {
	@Test
	void capacitiesFollowAuthoredStoneSize() {
		assertEquals(200, ArtifactEnergyReservoir.capacity("artifact_soulstone.small"));
		assertEquals(400, ArtifactEnergyReservoir.capacity("artifact_soulstone.medium"));
		assertEquals(800, ArtifactEnergyReservoir.capacity("artifact_soulstone.large"));
		assertEquals(1_600, ArtifactEnergyReservoir.capacity("artifact_soulmatrix"));
	}

	@Test
	void debitIsDeterministicAndAllOrNothing() {
		ArtifactEnergyReservoir.Debit paid = ArtifactEnergyReservoir.debit(List.of(30, 50, 100), 70);
		assertTrue(paid.paid());
		assertEquals(List.of(0, 10, 100), paid.balances());

		ArtifactEnergyReservoir.Debit refused = ArtifactEnergyReservoir.debit(List.of(10, 20), 31);
		assertFalse(refused.paid());
		assertEquals(List.of(10, 20), refused.balances());
	}

	@Test
	void storedEnergyIsAlwaysClampedToItsReservoir() {
		assertEquals(0, ArtifactEnergyReservoir.clamp("artifact_soulstone.small", -1));
		assertEquals(200, ArtifactEnergyReservoir.clamp("artifact_soulstone.small", 999));
	}
}

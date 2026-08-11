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

	@Test
	void transferPreviewIsExactAndAtomicInEitherDirection() {
		assertEquals(new ArtifactEnergyReservoir.Transfer(true, 40, 160, 60),
				ArtifactEnergyReservoir.transfer(100, 200, 100, 200, -60));
		assertEquals(new ArtifactEnergyReservoir.Transfer(true, 180, 20, 80),
				ArtifactEnergyReservoir.transfer(100, 200, 100, 200, 80));
		assertEquals(new ArtifactEnergyReservoir.Transfer(false, 100, 100, 0),
				ArtifactEnergyReservoir.transfer(100, 200, 100, 200, 201));
	}

	@Test
	void pendingCastShortfallUsesAuthoritativeCombinedBalances() {
		assertEquals(0, ArtifactEnergyReservoir.shortfall(90, 40, 80));
		assertEquals(30, ArtifactEnergyReservoir.shortfall(90, 40, 160));
	}

	@Test
	void serverPresetNeverAcceptsAClientAuthoredQuantity() {
		assertEquals(new ArtifactEnergyReservoir.Transfer(true, 0, 190, 90),
				ArtifactEnergyReservoir.transferStep(90, 200, 100, 200,
						ArtifactEnergyReservoir.Direction.STORE));
		assertEquals(new ArtifactEnergyReservoir.Transfer(true, 190, 0, 100),
				ArtifactEnergyReservoir.transferStep(90, 200, 100, 200,
						ArtifactEnergyReservoir.Direction.RELEASE));
	}
}

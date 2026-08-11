package com.powers.player;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnergyHistoryLedgerTest {
	@Test
	void authoritativeDeltasReconcileBySource() {
		EnergyHistoryLedger ledger = new EnergyHistoryLedger();
		UUID owner = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
		ledger.record(owner, 10, EnergyHistorySource.PLAYER_POOL_COST, 100, 70);
		ledger.record(owner, 10, EnergyHistorySource.RESERVOIR_COST, 40, 25);
		ledger.record(owner, 11, EnergyHistorySource.REGENERATION, 70, 76);

		EnergyHistorySnapshot snapshot = ledger.snapshot(owner);
		assertEquals(45, snapshot.consumed());
		assertEquals(6, snapshot.restored());
		assertEquals(30, snapshot.amount(EnergyHistorySource.PLAYER_POOL_COST));
		assertEquals(15, snapshot.amount(EnergyHistorySource.RESERVOIR_COST));
		assertEquals(6, snapshot.amount(EnergyHistorySource.REGENERATION));
		assertTrue(snapshot.reconciles());
		assertTrue(snapshot.tooltip().contains("spent=45 restored=6"));
	}

	@Test
	void historyAndSourceBreakdownStayBounded() {
		EnergyHistoryLedger ledger = new EnergyHistoryLedger();
		UUID owner = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
		for (int index = 0; index < 100; index++) {
			ledger.record(owner, index, EnergyHistorySource.DIRECT_DRAIN, 100, 99);
		}

		assertEquals(EnergyHistoryLedger.HISTORY_LIMIT, ledger.snapshot(owner).history().size());
		assertEquals(EnergyHistorySource.values().length, ledger.snapshot(owner).breakdown().size());
	}
}

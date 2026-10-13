package com.powers.power.damage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayeredDamageLedgerTest {
	@Test
	void recordsDistinctRequestedBlockedForwardedAndHealthTotals() {
		LayeredDamageLedger ledger = new LayeredDamageLedger();
		ledger.recordIncoming(50_000.0F);
		ledger.recordWard(LayeredDamageResolver.ward(50_000.0F, 40.0F, true));
		ledger.recordMirror(LayeredDamageResolver.mirror(8.0F, 1.5, 10.0F), 6.0F);
		ledger.recordHealthTransition(20.0F, 16.0F);

		var snapshot = ledger.snapshot();
		assertEquals(50_000.0, snapshot.incomingDamage());
		assertEquals(50_000.0, snapshot.wardBlockedDamage());
		assertEquals(40.0, snapshot.wardIntegritySpent());
		assertEquals(12.0, snapshot.mirrorRequestedDamage());
		assertEquals(10.0, snapshot.mirrorForwardedDamage());
		assertEquals(6.0, snapshot.mirrorActualHealthRemoved());
		assertEquals(4.0, snapshot.actualHealthRemoved());
		assertTrue(snapshot.line().contains("order=forcefield>soul_link>health"));
		assertTrue(snapshot.line().contains("wardBlocked=50000.0"));
		assertTrue(snapshot.line().contains("mirrorActualHealthRemoved=6.0"));
		assertTrue(snapshot.line().contains("actualHealthRemoved=4.0"));
		assertFalse(snapshot.line().contains("recursiveMirrorsSuppressed"));
	}

	@Test
	void recordsArmorReducedAndLethalTransitionsAsActualHealthRemoved() {
		LayeredDamageLedger ledger = new LayeredDamageLedger();

		ledger.recordHealthTransition(20.0F, 14.5F);
		ledger.recordHealthTransition(3.0F, 0.0F);

		assertEquals(8.5, ledger.snapshot().actualHealthRemoved());
	}

	@Test
	void clearStartsAWholeNewDiagnosticWindow() {
		LayeredDamageLedger ledger = new LayeredDamageLedger();
		ledger.recordHealthTransition(8.0F, 3.0F);
		ledger.clear();

		assertEquals(0.0, ledger.snapshot().actualHealthRemoved());
	}
}

package com.powers.power.damage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayeredDamageLedgerTest {
	@Test
	void recordsDistinctRequestedBlockedForwardedAndHealthTotals() {
		LayeredDamageLedger ledger = new LayeredDamageLedger();
		ledger.recordIncoming(50_000.0F);
		ledger.recordWard(LayeredDamageResolver.ward(50_000.0F, 40.0F, true));
		ledger.recordMirror(LayeredDamageResolver.mirror(8.0F, 1.5, 10.0F), 6.0F);
		ledger.recordHealth(4.0F);
		ledger.recordRecursiveMirrorSuppressed();

		var snapshot = ledger.snapshot();
		assertEquals(50_000.0, snapshot.incomingDamage());
		assertEquals(50_000.0, snapshot.wardBlockedDamage());
		assertEquals(40.0, snapshot.wardIntegritySpent());
		assertEquals(12.0, snapshot.mirrorRequestedDamage());
		assertEquals(10.0, snapshot.mirrorForwardedDamage());
		assertEquals(6.0, snapshot.mirrorHealthDamage());
		assertEquals(4.0, snapshot.healthDamage());
		assertEquals(1L, snapshot.recursiveMirrorsSuppressed());
		assertTrue(snapshot.line().contains("order=forcefield>soul_link>health"));
		assertTrue(snapshot.line().contains("wardBlocked=50000.0"));
	}

	@Test
	void clearStartsAWholeNewDiagnosticWindow() {
		LayeredDamageLedger ledger = new LayeredDamageLedger();
		ledger.recordHealth(5.0F);
		ledger.clear();

		assertEquals(0.0, ledger.snapshot().healthDamage());
	}
}

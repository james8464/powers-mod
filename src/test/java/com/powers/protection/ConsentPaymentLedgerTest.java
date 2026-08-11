package com.powers.protection;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsentPaymentLedgerTest {
	private static final UUID CASTER = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
	private static final UUID TARGET = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

	@Test
	void duplicateAndCrossCategoryPaymentsStayIsolatedWithinOneTick() {
		ConsentPaymentLedger ledger = new ConsentPaymentLedger();
		assertTrue(ledger.requiresPayment(10, CASTER, TARGET, ConsentKind.TELEPORT));
		ledger.recordPayment(10, CASTER, TARGET, ConsentKind.TELEPORT);
		assertFalse(ledger.requiresPayment(10, CASTER, TARGET, ConsentKind.TELEPORT));
		assertTrue(ledger.requiresPayment(10, CASTER, TARGET, ConsentKind.LOCATOR));
		assertTrue(ledger.requiresPayment(10, CASTER, UUID.randomUUID(), ConsentKind.TELEPORT));
	}

	@Test
	void nextTickRequiresANewPaymentAndClearCannotLeak() {
		ConsentPaymentLedger ledger = new ConsentPaymentLedger();
		ledger.recordPayment(10, CASTER, TARGET, ConsentKind.POSSESSION);
		assertTrue(ledger.requiresPayment(11, CASTER, TARGET, ConsentKind.POSSESSION));
		ledger.recordPayment(11, CASTER, TARGET, ConsentKind.POSSESSION);
		ledger.clear();
		assertTrue(ledger.requiresPayment(11, CASTER, TARGET, ConsentKind.POSSESSION));
	}
}

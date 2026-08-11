package com.powers.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperatorAuditLedgerTest {
	@Test
	void aggregationRetainsCountsOnlyInABoundedClosedKeyspace() {
		OperatorAuditLedger ledger = new OperatorAuditLedger();
		ledger.record(new OperatorAuditEvent(OperatorAuditAction.RECOVERY,
				OperatorAuditResult.SUCCESS, "Alice", "Bob", "body"));
		ledger.record(new OperatorAuditEvent(OperatorAuditAction.RECOVERY,
				OperatorAuditResult.SUCCESS, "Mallory", "Eve", "body"));
		ledger.record(new OperatorAuditEvent(OperatorAuditAction.FORCED_TRAVEL,
				OperatorAuditResult.DENIED, "Alice", "Alice", "unknown_dimension"));

		OperatorAuditSnapshot snapshot = ledger.snapshot();
		assertEquals(3, snapshot.total());
		assertEquals(2, snapshot.count(OperatorAuditAction.RECOVERY, OperatorAuditResult.SUCCESS));
		assertEquals(1, snapshot.count(OperatorAuditAction.FORCED_TRAVEL, OperatorAuditResult.DENIED));
		assertEquals(OperatorAuditAction.values().length * OperatorAuditResult.values().length,
				snapshot.counts().size());
		assertEquals("", snapshot.counts().stream().map(OperatorAuditSnapshot.Count::toString)
				.filter(text -> text.contains("Alice") || text.contains("Bob")).findFirst().orElse(""));
	}
}

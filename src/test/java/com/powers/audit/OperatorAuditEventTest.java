package com.powers.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorAuditEventTest {
	@Test
	void structuredLineStripsLogInjectionAndBoundsIdentifyingFields() {
		OperatorAuditEvent event = new OperatorAuditEvent(OperatorAuditAction.CONSENT_OVERRIDE,
				OperatorAuditResult.SUCCESS, "op\nname", "target\rname", "teleport\t" + "x".repeat(200));

		String line = event.structuredLine();
		assertFalse(line.contains("\n"));
		assertFalse(line.contains("\r"));
		assertFalse(line.contains("\t"));
		assertTrue(line.startsWith("powers_operator_audit action=consent_override result=success"));
		assertTrue(line.contains("actor=op_name"));
		assertTrue(line.contains("subject=target_name"));
		assertTrue(line.length() <= 260);
	}

	@Test
	void privilegedHooksUseAClosedSetOfAggregateActions() {
		assertEquals(java.util.EnumSet.of(OperatorAuditAction.CONSENT_OVERRIDE,
				OperatorAuditAction.RECOVERY, OperatorAuditAction.FORCED_TRAVEL,
				OperatorAuditAction.TESTING_CONTROL, OperatorAuditAction.CATASTROPHIC_RITUAL),
				java.util.EnumSet.allOf(OperatorAuditAction.class));
	}
}

package com.powers.audit;

import com.powers.PowersMod;

/** Runtime bridge that emits structured server logs and keeps only bounded counts. */
public final class OperatorAudit {
	private static final OperatorAuditLedger LEDGER = new OperatorAuditLedger();

	private OperatorAudit() {
	}

	public static void record(OperatorAuditAction action, OperatorAuditResult result,
			String actor, String subject, String detail) {
		OperatorAuditEvent event = new OperatorAuditEvent(action, result, actor, subject, detail);
		LEDGER.record(event);
		PowersMod.LOGGER.info(event.structuredLine());
	}

	public static OperatorAuditSnapshot snapshot() {
		return LEDGER.snapshot();
	}

	public static void clear() {
		LEDGER.clear();
	}
}

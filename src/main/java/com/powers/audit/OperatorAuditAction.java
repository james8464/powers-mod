package com.powers.audit;

/** Closed privileged-action keyspace used by logs and redacted diagnostics. */
public enum OperatorAuditAction {
	CONSENT_OVERRIDE,
	RECOVERY,
	FORCED_TRAVEL,
	TESTING_CONTROL,
	CATASTROPHIC_RITUAL
}

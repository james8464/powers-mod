package com.powers.protection;

/** Auditable outcome of a magical protection-policy evaluation. */
public enum ProtectionDecision {
	ALLOW,
	DENY_SAFE_ZONE,
	DENY_CONSENT,
	DENY_TERRAIN,
	DENY_BLOCK_ENTITY,
	DENY_ADAPTER
}

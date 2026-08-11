package com.powers.player;

/** Closed source keyspace for authoritative player energy transactions. */
public enum EnergyHistorySource {
	PLAYER_POOL_COST,
	RESERVOIR_COST,
	REFUND,
	REGENERATION,
	SLEEP_RESTORE,
	OPERATOR_RESTORE,
	DIRECT_DRAIN,
	EMPTY,
	TRANSACTION_ROLLBACK
}

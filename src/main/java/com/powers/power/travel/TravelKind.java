package com.powers.power.travel;

/** Authority and consent category of a magical movement request. */
public enum TravelKind {
	POWER,
	CRYSTAL,
	PROJECTION,
	COMPANION,
	PLAYER_RETURN,
	/** Internal fatal-mind path; never accepted from packets, powers, items, or commands. */
	FATAL_SOUL_RETURN,
	ADMIN_RECOVERY
}

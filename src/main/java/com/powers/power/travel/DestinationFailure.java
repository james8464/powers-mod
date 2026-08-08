package com.powers.power.travel;

/** Exhaustive server-side reasons a requested magical destination is unsafe. */
public enum DestinationFailure {
	NONE,
	OUT_OF_BOUNDS,
	UNLOADED_CHUNK,
	COLLISION,
	HAZARD,
	WARD,
	ANTI_PORTAL,
	SAFE_ZONE,
	ANCHOR,
	REALM_RESTRICTED
}

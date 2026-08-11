package com.powers.protection;

import java.util.Set;

/** One safety-first order shared by simultaneous realm, ward, shield, and consent conflicts. */
public final class CrossSystemPrecedence {
	public enum Guard {
		REALM_CONFINEMENT,
		SAFE_ZONE,
		AMETHYST,
		FORCEFIELD,
		ANTI_PORTAL_FIELD,
		DIMENSIONAL_ANCHOR,
		CONSENT_OVERRIDE
	}

	private CrossSystemPrecedence() {
	}

	/** Returns the first applicable guard, following declaration order, or null. */
	public static Guard first(Set<Guard> applicable) {
		if (applicable == null || applicable.isEmpty()) return null;
		for (Guard guard : Guard.values()) if (applicable.contains(guard)) return guard;
		return null;
	}
}

package com.powers.realm;

import java.util.Set;

/** Exact identifiers for POWERS-owned mindscape dimensions. */
public final class RealmDimensionRules {
	private static final Set<String> MINDSCAPES = Set.of(
			"powers:dark_realm", "powers:light_realm", "powers:middleworld");

	private RealmDimensionRules() {
	}

	public static boolean isMindscape(String dimensionId) {
		return dimensionId != null && MINDSCAPES.contains(dimensionId);
	}
}

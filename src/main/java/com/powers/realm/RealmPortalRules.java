package com.powers.realm;

/** Realm departure gate used by every vanilla portal implementation. */
public final class RealmPortalRules {
	private RealmPortalRules() {
	}

	/** A portal cannot become an ordinary-player escape hatch from a mindscape. */
	public static boolean mayDepart(String origin, boolean darknessTag,
			int normalLevel, int darknessLevel) {
		return RealmConfinementRules.requiredRespawnRealm(origin, darknessTag,
				normalLevel, darknessLevel) == null;
	}
}

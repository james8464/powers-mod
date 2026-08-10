package com.powers.realm;

import com.powers.player.SkillSystem;

/** Pure departure policy shared by travel validation and death respawning. */
public final class RealmConfinementRules {
	public enum Enforcement { ALLOW, TRANSFER, LOCKED_HOLD }

	private RealmConfinementRules() {
	}

	/**
	 * Returns the realm a death must return to, or {@code null} when ordinary
	 * respawning is allowed. Both the darkness tag and rank are required for
	 * Dark Realm departure; either rank ladder can unlock Light Realm departure.
	 */
	public static String requiredRespawnRealm(String origin, boolean darknessTag,
			int normalLevel, int darknessLevel) {
		if ("powers:dark_realm".equals(origin)
				&& (!darknessTag || darknessLevel < SkillSystem.DARKNESS_GATE_LEVEL)) {
			return origin;
		}
		if ("powers:light_realm".equals(origin)
				&& Math.max(normalLevel, darknessLevel) < SkillSystem.DARKNESS_GATE_LEVEL) {
			return origin;
		}
		return null;
	}

	/** Missing required dimensions must fail closed until an administrator repairs the world. */
	public static Enforcement enforcement(String requiredRealm, boolean destinationAvailable) {
		if (requiredRealm == null) return Enforcement.ALLOW;
		return destinationAvailable ? Enforcement.TRANSFER : Enforcement.LOCKED_HOLD;
	}
}

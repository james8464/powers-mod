package com.powers.compat;

/** Null-safe bounded rules used at projectile, damage and effect integration boundaries. */
public final class ThirdPartyCombatCompatibility {
	private ThirdPartyCombatCompatibility() { }

	public static CombatCompatibilityDisposition projectile(boolean powersOwned) {
		return powersOwned ? CombatCompatibilityDisposition.POWERS_OWNED
				: CombatCompatibilityDisposition.FOREIGN_UNCHANGED;
	}

}

package com.powers.compat;

/** Null-safe bounded rules used at projectile, damage and effect integration boundaries. */
public final class ThirdPartyCombatCompatibility {
	private static final int MAX_EFFECT_TICKS = 20 * 60 * 60;

	private ThirdPartyCombatCompatibility() { }

	public static CombatCompatibilityDisposition projectile(boolean powersOwned) {
		return powersOwned ? CombatCompatibilityDisposition.POWERS_OWNED
				: CombatCompatibilityDisposition.FOREIGN_UNCHANGED;
	}

	public static CombatCompatibilityDisposition damage(String typeId) {
		return "powers:power_magic".equals(typeId) || "powers:celestial_ruin".equals(typeId)
				? CombatCompatibilityDisposition.POWERS_OWNED
				: CombatCompatibilityDisposition.FOREIGN_UNCHANGED;
	}

	public static int effectDuration(int ticks) {
		return Math.clamp(ticks, 0, MAX_EFFECT_TICKS);
	}
}

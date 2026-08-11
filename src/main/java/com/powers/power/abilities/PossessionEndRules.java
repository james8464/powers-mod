package com.powers.power.abilities;

/** Pure priority and bounded punishment values for controlled-vessel termination. */
public final class PossessionEndRules {
	public static final int WRATH_TICKS = 200;
	private static final int MIN_ENERGY_DRAIN = 25;
	private static final int MAX_ENERGY_DRAIN = 200;
	private static final float MAX_DAMAGE = 12.0F;

	public enum Reason {
		NONE, VESSEL_FATAL, TARGET_UNAVAILABLE, OWNER_INVALID, LOCATION_INVALID,
		PROTECTION_LOST, SOURCE_LOST, EXPIRED
	}

	private PossessionEndRules() {
	}

	/** Fatal vessel loss takes priority so simultaneous expiry cannot suppress wrath. */
	public static Reason reason(boolean targetAlive, boolean targetAvailable,
			boolean ownerValid, boolean locationValid, boolean protectionValid,
			boolean sourceValid, boolean beforeExpiry) {
		if (!targetAlive) return Reason.VESSEL_FATAL;
		if (!targetAvailable) return Reason.TARGET_UNAVAILABLE;
		if (!ownerValid) return Reason.OWNER_INVALID;
		if (!locationValid) return Reason.LOCATION_INVALID;
		if (!protectionValid) return Reason.PROTECTION_LOST;
		if (!sourceValid) return Reason.SOURCE_LOST;
		if (!beforeExpiry) return Reason.EXPIRED;
		return Reason.NONE;
	}

	/** Drains 35% with caps so custom capacities cannot cause overflow or excessive writes. */
	public static int wrathEnergyDrain(int capacity) {
		if (capacity <= 0) return 0;
		long proportional = Math.round(Math.ceil(capacity * 0.35D));
		return (int) Math.min(capacity, Math.clamp(proportional,
				MIN_ENERGY_DRAIN, MAX_ENERGY_DRAIN));
	}

	/** Leaves at least one health so wrath itself never replaces the requested return semantics. */
	public static float wrathDamage(float health) {
		if (!Float.isFinite(health) || health <= 1.0F) return 0.0F;
		return Math.min(MAX_DAMAGE, health - 1.0F);
	}
}

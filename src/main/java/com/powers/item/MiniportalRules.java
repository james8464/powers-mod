package com.powers.item;

/** Pure charge and destination rules for the finite-use Miniportal. */
public final class MiniportalRules {
	public static final int MAX_CHARGES = 2;

	private MiniportalRules() {
	}

	public static int charges(Integer stored) {
		return stored == null ? MAX_CHARGES : Math.clamp(stored, 0, MAX_CHARGES);
	}

	public static boolean mayTravel(int charges, boolean sameDimension) {
		return sameDimension && charges > 0;
	}

	/** Prevents dropping, replacing, or externally editing a device mid-load. */
	public static boolean mayCommit(boolean samePlayer, boolean alive,
			boolean sameOrigin, boolean ownsDevice, int reservedCharges, int currentCharges) {
		return samePlayer && alive && sameOrigin && ownsDevice
				&& reservedCharges > 0 && reservedCharges == currentCharges;
	}

	public static int afterSuccessfulTravel(int charges) {
		return Math.max(0, Math.min(MAX_CHARGES, charges) - 1);
	}

	public static int afterRecharge() {
		return MAX_CHARGES;
	}

	/** Thirteen-pixel vanilla durability-bar width, rounded to show one half as seven. */
	public static int barWidth(int charges) {
		return Math.round(13.0F * Math.clamp(charges, 0, MAX_CHARGES) / MAX_CHARGES);
	}
}

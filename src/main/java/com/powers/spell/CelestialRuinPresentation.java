package com.powers.spell;

/** Shared client/server lease and accessibility timing for Heavenfall presentation. */
public final class CelestialRuinPresentation {
	public static final int BEAM_REFRESH_TICKS = 20;
	public static final int BEAM_LEASE_TICKS = 35;
	public static final int FLASH_TICKS = 60;

	private CelestialRuinPresentation() {
	}

	public static int flashAlpha(int remainingTicks) {
		if (remainingTicks <= 0) return 0;
		if (remainingTicks > 40) return 255;
		return Math.clamp((int) Math.round(255.0 * remainingTicks / 40.0), 0, 255);
	}
}

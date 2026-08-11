package com.powers.spell;

/** Shared client/server lease and accessibility timing for Heavenfall presentation. */
public final class CelestialRuinPresentation {
	public static final int BEAM_REFRESH_TICKS = 20;
	public static final int BEAM_LEASE_TICKS = 35;
	public static final int BEAM_VIEW_RADIUS = 1_024;
	public static final int BEAM_VERTICAL_SLICES = 4;
	public static final int BEAM_PARTICLES_PER_SLICE = 16;
	public static final int BEAM_BOUNDARY_PARTICLES = 24;
	public static final int FLASH_TICKS = 60;
	public static final int RINGING_TICKS = 100;

	private CelestialRuinPresentation() {
	}

	public static int flashAlpha(int remainingTicks) {
		if (remainingTicks <= 0) return 0;
		if (remainingTicks > 40) return 255;
		return Math.clamp((int) Math.round(255.0 * remainingTicks / 40.0), 0, 255);
	}

	/** Exact local particle ceiling for one visible warning column per client tick. */
	public static int clientBeamParticleCount() {
		return BEAM_VERTICAL_SLICES * BEAM_PARTICLES_PER_SLICE
				+ BEAM_PARTICLES_PER_SLICE / 2 + BEAM_BOUNDARY_PARTICLES;
	}

	/** Ears-ringing volume fades quadratically after the whiteout. */
	public static float ringingVolume(int remainingTicks) {
		if (remainingTicks <= 0) return 0.0F;
		double fraction = Math.min(1.0, remainingTicks / (double) RINGING_TICKS);
		return (float) (fraction * fraction);
	}
}

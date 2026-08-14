package com.powers.spell;

import com.powers.fx.FxLodTier;

/** Shared client/server lease and accessibility timing for Heavenfall presentation. */
public final class CelestialRuinPresentation {
	public static final int BEAM_REFRESH_TICKS = 20;
	public static final int BEAM_LEASE_TICKS = 35;
	public static final int BEAM_VIEW_RADIUS = 6_000;
	public static final int BEAM_VERTICAL_SLICES = 12;
	public static final int BEAM_PARTICLES_PER_SLICE = 8;
	public static final int BEAM_BOUNDARY_PARTICLES = 24;
	public static final int FLASH_TICKS = 400;
	public static final int RINGING_TICKS = 500;
	private static final int OPAQUE_TICKS = 60;

	private CelestialRuinPresentation() {
	}

	public static int flashAlpha(int remainingTicks) {
		if (remainingTicks <= 0) return 0;
		int fadeTicks = FLASH_TICKS - OPAQUE_TICKS;
		if (remainingTicks > fadeTicks) return 255;
		return Math.clamp((int) Math.round(255.0 * remainingTicks / fadeTicks), 0, 255);
	}

	/** Exact local particle ceiling for one visible warning column per client tick. */
	public static int clientBeamParticleCount() {
		return BEAM_VERTICAL_SLICES * BEAM_PARTICLES_PER_SLICE
				+ BEAM_PARTICLES_PER_SLICE / 2 + BEAM_BOUNDARY_PARTICLES;
	}

	/** Keeps the hundred-block diameter and sky-height span at every distance tier. */
	public static ColumnDensity columnDensity(FxLodTier tier) {
		return switch (tier) {
			case NEAR -> new ColumnDensity(12, 8, 24);
			case MID -> new ColumnDensity(8, 4, 16);
			case FAR -> new ColumnDensity(6, 2, 12);
			case HIDDEN -> new ColumnDensity(0, 0, 0);
		};
	}

	/** Ears-ringing volume fades quadratically after the whiteout. */
	public static float ringingVolume(int remainingTicks) {
		if (remainingTicks <= 0) return 0.0F;
		double fraction = Math.min(1.0, remainingTicks / (double) RINGING_TICKS);
		return (float) (fraction * fraction);
	}

	/** Prevents the catastrophe's long tinnitus layer from overwhelming distant observers. */
	public static float audioGain(FxLodTier tier) {
		return switch (tier) {
			case NEAR -> 1.0F;
			case MID -> 0.55F;
			case FAR -> 0.28F;
			case HIDDEN -> 0.0F;
		};
	}

	public record ColumnDensity(int verticalSlices, int particlesPerSlice,
			int boundaryParticles) {
		public int particleCount() {
			return verticalSlices * particlesPerSlice
					+ particlesPerSlice / 2 + boundaryParticles;
		}
	}
}

package com.powers.spell;

/** Pure dimensions and work bounds for the Heavenfall cleanup ritual. */
public final class CelestialRuinRules {
	public static final int COUNTDOWN_TICKS = 1_200;
	public static final int BEAM_RADIUS = 50;
	public static final int BLAST_RADIUS = 120;
	public static final float PEAK_DAMAGE = 2_000.0f;
	public static final int BLOCKS_PER_TICK = 8_192;
	public static final int ENTITY_LIMIT = 1_024;

	private CelestialRuinRules() {
	}

	/** Exact integer-sphere boundary used by tests and the destructive cursor. */
	public static boolean insideBlast(int x, int y, int z) {
		return (long) x * x + (long) y * y + (long) z * z
				<= (long) BLAST_RADIUS * BLAST_RADIUS;
	}

	/** Quadratic damage falloff with a hard zero at the blast boundary. */
	public static float damage(double distance) {
		if (!Double.isFinite(distance) || distance < 0.0 || distance >= BLAST_RADIUS) {
			return 0.0f;
		}
		double remaining = 1.0 - distance / BLAST_RADIUS;
		return (float) (PEAK_DAMAGE * remaining * remaining);
	}
}

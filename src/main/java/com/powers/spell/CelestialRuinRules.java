package com.powers.spell;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Pure dimensions and work bounds for the Heavenfall cleanup ritual. */
public final class CelestialRuinRules {
	public static final int COUNTDOWN_TICKS = 1_200;
	public static final int BEAM_RADIUS = 50;
	public static final int BLAST_RADIUS = 120;
	public static final int DAMAGE_RADIUS = 6_000;
	public static final float PEAK_DAMAGE = 50_000.0f;
	public static final int BLOCKS_PER_TICK = 32_768;
	public static final int ENTITY_LIMIT = 4_096;
	public static final int AFTERSHOCK_RAYS = 96;
	public static final int AFTERSHOCK_STEP_BLOCKS = 4;
	public static final int AFTERSHOCK_WORK_PER_TICK = 2_048;

	public record AftershockOffset(int x, int z) {
	}

	private CelestialRuinRules() {
	}

	/** A server-wide Time Stop pauses both warning and destruction work. */
	public static boolean mayAdvance(boolean globallyStopped) {
		return !globallyStopped;
	}

	/** Exact integer-sphere boundary used by tests and the destructive cursor. */
	public static boolean insideBlast(int x, int y, int z) {
		return (long) x * x + (long) y * y + (long) z * z
				<= (long) BLAST_RADIUS * BLAST_RADIUS;
	}

	/** Quadratic living-damage shockwave reaching far beyond the terrain crater. */
	public static float damage(double distance) {
		if (!Double.isFinite(distance) || distance < 0.0 || distance >= DAMAGE_RADIUS) {
			return 0.0f;
		}
		double remaining = 1.0 - distance / DAMAGE_RADIUS;
		return (float) (PEAK_DAMAGE * remaining * remaining);
	}

	/** Boss-scale outward impulse, still finite at the epicentre and zero at the boundary. */
	public static double knockback(double distance) {
		if (!Double.isFinite(distance) || distance < 0.0 || distance >= DAMAGE_RADIUS) return 0.0;
		double remaining = 1.0 - distance / DAMAGE_RADIUS;
		return 18.0 * Math.pow(remaining, 0.65);
	}

	/** Bounds the shockwave horizontally while covering the complete build height. */
	public static AABB damageBounds(Vec3 epicenter, int minimumY, int maximumY) {
		return new AABB(epicenter.x - DAMAGE_RADIUS, minimumY,
				epicenter.z - DAMAGE_RADIUS, epicenter.x + DAMAGE_RADIUS,
				maximumY, epicenter.z + DAMAGE_RADIUS);
	}

	public static int aftershockTotalSteps() {
		return AFTERSHOCK_RAYS * (DAMAGE_RADIUS / AFTERSHOCK_STEP_BLOCKS);
	}

	/** Expanding deterministic streak geometry, evaluated without loading distant chunks. */
	public static AftershockOffset aftershockOffset(int index) {
		int bounded = Math.clamp(index, 0, aftershockTotalSteps() - 1);
		int ray = bounded % AFTERSHOCK_RAYS;
		int distanceStep = bounded / AFTERSHOCK_RAYS + 1;
		double angle = Math.PI * 2.0 * ray / AFTERSHOCK_RAYS
				+ Math.sin(ray * 12.9898) * 0.035;
		double distance = distanceStep * AFTERSHOCK_STEP_BLOCKS;
		return new AftershockOffset((int) Math.round(Math.cos(angle) * distance),
				(int) Math.round(Math.sin(angle) * distance));
	}

	/** Living-force cleanup is mandatory; other terrain follows its dedicated catastrophic policy. */
	public static boolean shouldDestroy(boolean livingForce, boolean terrainEnabled,
			boolean hasBlockEntity, boolean blockEntityDamageEnabled) {
		return livingForce || terrainEnabled && (!hasBlockEntity || blockEntityDamageEnabled);
	}

	public static boolean shouldIgnite(boolean terrainEnabled, boolean safeZone) {
		return terrainEnabled && !safeZone;
	}
}

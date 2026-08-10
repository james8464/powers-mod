package com.powers.power.abilities;

import com.powers.magic.runtime.CastSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Identifier-only, server-thread state for one finite Astral Convergence. */
final class AstralConvergence {
	final UUID owner;
	final ResourceKey<Level> dimension;
	final CastSource castSource;
	final long startedAt;
	final long expiresAt;
	final long seed;
	final Vec3 origin;
	final UUID trackedTarget;
	final double stormRadius;
	final float baseDamage;
	final int terrainTier;
	final int strikeCount;
	final boolean empoweredImpact;
	final boolean secondStep;
	final boolean trueSight;
	final boolean reflectiveWard;
	final boolean soulEcho;
	final boolean afterimage;
	final boolean ancientMastery;
	final Map<UUID, HitRecord> hits = new LinkedHashMap<>();
	Vec3 center;
	int nextStrike;
	boolean crownResolved;

	AstralConvergence(UUID owner, ResourceKey<Level> dimension, CastSource castSource,
			long startedAt, long expiresAt, long seed, Vec3 origin, UUID trackedTarget,
			double stormRadius, float baseDamage, int terrainTier, int strikeCount,
			boolean empoweredImpact, boolean secondStep, boolean trueSight,
			boolean reflectiveWard, boolean soulEcho, boolean afterimage,
			boolean ancientMastery) {
		this.owner = owner;
		this.dimension = dimension;
		this.castSource = castSource;
		this.startedAt = startedAt;
		this.expiresAt = expiresAt;
		this.seed = seed;
		this.origin = origin;
		this.center = origin;
		this.trackedTarget = trackedTarget;
		this.stormRadius = stormRadius;
		this.baseDamage = baseDamage;
		this.terrainTier = terrainTier;
		this.strikeCount = strikeCount;
		this.empoweredImpact = empoweredImpact;
		this.secondStep = secondStep;
		this.trueSight = trueSight;
		this.reflectiveWard = reflectiveWard;
		this.soulEcho = soulEcho;
		this.afterimage = afterimage;
		this.ancientMastery = ancientMastery;
	}

	/** One body's bounded repeat history within this storm only. */
	record HitRecord(long lastHit, int hits) {
	}
}

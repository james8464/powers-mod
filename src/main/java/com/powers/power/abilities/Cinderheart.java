package com.powers.power.abilities;

import com.powers.magic.runtime.CastSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Identifier-only, server-thread state for one finite Cinderheart lifecycle. */
final class Cinderheart {
	final UUID originalOwner;
	final UUID projectile;
	final CastSource castSource;
	final int terrainTier;
	final ResourceKey<Level> dimension;
	/** Owning-world game ticks, never server control ticks. */
	final long startedAt;
	UUID controller;
	long expiresAt;
	double potencyMultiplier;
	boolean empoweredImpact;
	boolean reflectiveWard;
	boolean afterimage;
	boolean trueSight;
	boolean ancientMastery;
	int tier = 1;
	int reflections;
	boolean launched;
	Vec3 lastPosition;

	Cinderheart(UUID originalOwner, UUID projectile, CastSource castSource, int terrainTier,
			ResourceKey<Level> dimension, long startedAt, long expiresAt,
			double potencyMultiplier, boolean empoweredImpact, boolean reflectiveWard,
			boolean afterimage, boolean trueSight, boolean ancientMastery,
			Vec3 lastPosition) {
		this.originalOwner = originalOwner;
		this.projectile = projectile;
		this.castSource = castSource;
		this.terrainTier = Math.clamp(terrainTier, 0, 10);
		this.dimension = dimension;
		this.startedAt = startedAt;
		this.controller = originalOwner;
		this.expiresAt = expiresAt;
		this.potencyMultiplier = potencyMultiplier;
		this.empoweredImpact = empoweredImpact;
		this.reflectiveWard = reflectiveWard;
		this.afterimage = afterimage;
		this.trueSight = trueSight;
		this.ancientMastery = ancientMastery;
		this.lastPosition = lastPosition;
	}
}
